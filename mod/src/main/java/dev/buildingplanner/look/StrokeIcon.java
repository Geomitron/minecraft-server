package dev.buildingplanner.look;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Draws a line icon from its SVG elements, as Lucide gives them, into a square of pixels: on Lucide's 24-unit square,
 * each element's outline at a stroke 2 units wide with round ends and joins. It reads {@code path} (the commands M, L,
 * H, V, C, S, Q, A and Z, in either case), {@code rect} (with {@code rx} and {@code ry}) and {@code circle}, which are
 * all Lucide's icons here use. Each pixel's coverage is the share of a 4 by 4 grid of points inside it that the stroke
 * covers.
 */
public final class StrokeIcon {
	/** The side of the square the elements are drawn on, in their units. */
	private static final double VIEW = 24;
	private static final double HALF_STROKE = 1;
	private static final int SAMPLES = 4;
	private static final int CURVE_STEPS = 16;
	private static final Pattern ELEMENT = Pattern.compile("<(\\w+)\\s([^>]*?)/?>");
	private static final Pattern ATTRIBUTE = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");

	private StrokeIcon() {
	}

	/** Each pixel's coverage from 0 to 255, row by row from the top, of a {@code size} by {@code size} square. */
	public static int[] rasterize(List<String> elements, int size) {
		List<double[]> segments = segments(elements);
		int[] coverage = new int[size * size];
		double unit = VIEW / size;
		double reach = HALF_STROKE * HALF_STROKE;
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				int hits = 0;
				for (int sy = 0; sy < SAMPLES; sy++) {
					for (int sx = 0; sx < SAMPLES; sx++) {
						double x = (px + (sx + 0.5) / SAMPLES) * unit;
						double y = (py + (sy + 0.5) / SAMPLES) * unit;
						for (double[] s : segments) {
							if (distanceSquared(x, y, s) <= reach) {
								hits++;
								break;
							}
						}
					}
				}
				coverage[py * size + px] = hits * 255 / (SAMPLES * SAMPLES);
			}
		}
		return coverage;
	}

	/** The elements' outlines as straight segments, each {x1, y1, x2, y2}. */
	static List<double[]> segments(List<String> elements) {
		List<double[]> segments = new ArrayList<>();
		for (String element : elements) {
			Matcher m = ELEMENT.matcher(element.strip());
			if (!m.matches()) {
				throw new IllegalArgumentException("Not an SVG element: " + element);
			}
			Map<String, String> a = new HashMap<>();
			Matcher am = ATTRIBUTE.matcher(m.group(2));
			while (am.find()) {
				a.put(am.group(1), am.group(2));
			}
			String d = switch (m.group(1)) {
				case "path" -> a.get("d");
				case "rect" -> rect(a);
				case "circle" -> circle(a);
				default -> throw new IllegalArgumentException("Can't draw <" + m.group(1) + ">");
			};
			new PathReader(d, segments).read();
		}
		return segments;
	}

	private static String rect(Map<String, String> a) {
		double x = number(a, "x", 0);
		double y = number(a, "y", 0);
		double w = number(a, "width", 0);
		double h = number(a, "height", 0);
		double rx = number(a, "rx", number(a, "ry", 0));
		double ry = number(a, "ry", rx);
		rx = Math.min(rx, w / 2);
		ry = Math.min(ry, h / 2);
		String corner = " A" + rx + " " + ry + " 0 0 1 ";
		return "M" + (x + rx) + " " + y + " H" + (x + w - rx) + corner + (x + w) + " " + (y + ry)
			+ " V" + (y + h - ry) + corner + (x + w - rx) + " " + (y + h) + " H" + (x + rx) + corner + x + " "
			+ (y + h - ry) + " V" + (y + ry) + corner + (x + rx) + " " + y + " Z";
	}

	private static String circle(Map<String, String> a) {
		double cx = number(a, "cx", 0);
		double cy = number(a, "cy", 0);
		double r = number(a, "r", 0);
		String half = " A" + r + " " + r + " 0 1 0 ";
		return "M" + (cx - r) + " " + cy + half + (cx + r) + " " + cy + half + (cx - r) + " " + cy + " Z";
	}

	private static double number(Map<String, String> a, String name, double otherwise) {
		String value = a.get(name);
		return value == null ? otherwise : Double.parseDouble(value);
	}

	private static double distanceSquared(double x, double y, double[] s) {
		double dx = s[2] - s[0];
		double dy = s[3] - s[1];
		double length = dx * dx + dy * dy;
		double t = length == 0 ? 0 : Math.clamp(((x - s[0]) * dx + (y - s[1]) * dy) / length, 0, 1);
		double ex = s[0] + t * dx - x;
		double ey = s[1] + t * dy - y;
		return ex * ex + ey * ey;
	}

	/** Reads one path's data into straight segments, curves and arcs cut into short ones. */
	static final class PathReader {
		private final String d;
		private final List<double[]> out;
		private int at;
		private double x;
		private double y;
		private double startX;
		private double startY;
		/** The last curve's second control point, for S and Q's reflection; NaN after any other command. */
		private double controlX = Double.NaN;
		private double controlY = Double.NaN;

		PathReader(String d, List<double[]> out) {
			this.d = d;
			this.out = out;
		}

		void read() {
			char command = 0;
			while (skipSeparators() < d.length()) {
				char c = d.charAt(at);
				if (Character.isLetter(c)) {
					command = c;
					at++;
				} else if (command == 0) {
					throw new IllegalArgumentException("Path data starts with a number: " + d);
				}
				command = draw(command);
			}
		}

		/** Draws one command's arguments; returns the command that further arguments without a letter repeat. */
		private char draw(char command) {
			boolean relative = Character.isLowerCase(command);
			double ox = relative ? x : 0;
			double oy = relative ? y : 0;
			char next = command;
			boolean curve = false;
			switch (Character.toUpperCase(command)) {
				case 'M' -> {
					x = ox + number();
					y = oy + number();
					startX = x;
					startY = y;
					next = relative ? 'l' : 'L';
				}
				case 'L' -> lineTo(ox + number(), oy + number());
				case 'H' -> lineTo(ox + number(), y);
				case 'V' -> lineTo(x, oy + number());
				case 'C' -> {
					double x1 = ox + number();
					double y1 = oy + number();
					double x2 = ox + number();
					double y2 = oy + number();
					cubicTo(x1, y1, x2, y2, ox + number(), oy + number());
					curve = true;
				}
				case 'S' -> {
					double x1 = Double.isNaN(controlX) ? x : 2 * x - controlX;
					double y1 = Double.isNaN(controlY) ? y : 2 * y - controlY;
					double x2 = ox + number();
					double y2 = oy + number();
					cubicTo(x1, y1, x2, y2, ox + number(), oy + number());
					curve = true;
				}
				case 'Q' -> {
					double qx = ox + number();
					double qy = oy + number();
					double ex = ox + number();
					double ey = oy + number();
					cubicTo(x + 2.0 / 3 * (qx - x), y + 2.0 / 3 * (qy - y), ex + 2.0 / 3 * (qx - ex),
						ey + 2.0 / 3 * (qy - ey), ex, ey);
				}
				case 'A' -> {
					double rx = number();
					double ry = number();
					double rotation = number();
					boolean large = flag();
					boolean sweep = flag();
					arcTo(rx, ry, rotation, large, sweep, ox + number(), oy + number());
				}
				case 'Z' -> {
					lineTo(startX, startY);
					next = 0;
				}
				default -> throw new IllegalArgumentException("Can't draw path command " + command);
			}
			if (!curve) {
				controlX = Double.NaN;
				controlY = Double.NaN;
			}
			return next;
		}

		private void lineTo(double toX, double toY) {
			out.add(new double[] {x, y, toX, toY});
			x = toX;
			y = toY;
		}

		private void cubicTo(double x1, double y1, double x2, double y2, double toX, double toY) {
			double fromX = x;
			double fromY = y;
			for (int i = 1; i <= CURVE_STEPS; i++) {
				double t = (double) i / CURVE_STEPS;
				double u = 1 - t;
				lineTo(u * u * u * fromX + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * toX,
					u * u * u * fromY + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * toY);
			}
			controlX = x2;
			controlY = y2;
		}

		/** An SVG arc, from its end points to its centre as the SVG specification's appendix on arcs gives it. */
		private void arcTo(double rx, double ry, double rotation, boolean large, boolean sweep, double toX, double toY) {
			rx = Math.abs(rx);
			ry = Math.abs(ry);
			if (rx == 0 || ry == 0 || x == toX && y == toY) {
				lineTo(toX, toY);
				return;
			}
			double phi = Math.toRadians(rotation);
			double cos = Math.cos(phi);
			double sin = Math.sin(phi);
			double hx = (x - toX) / 2;
			double hy = (y - toY) / 2;
			double x1 = cos * hx + sin * hy;
			double y1 = -sin * hx + cos * hy;
			double lambda = x1 * x1 / (rx * rx) + y1 * y1 / (ry * ry);
			if (lambda > 1) {
				rx *= Math.sqrt(lambda);
				ry *= Math.sqrt(lambda);
			}
			double numerator = rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1;
			double denominator = rx * rx * y1 * y1 + ry * ry * x1 * x1;
			double coefficient = Math.sqrt(Math.max(0, numerator / denominator)) * (large == sweep ? -1 : 1);
			double cx1 = coefficient * rx * y1 / ry;
			double cy1 = -coefficient * ry * x1 / rx;
			double cx = cos * cx1 - sin * cy1 + (x + toX) / 2;
			double cy = sin * cx1 + cos * cy1 + (y + toY) / 2;
			double start = Math.atan2((y1 - cy1) / ry, (x1 - cx1) / rx);
			double delta = Math.atan2((-y1 - cy1) / ry, (-x1 - cx1) / rx) - start;
			if (sweep && delta < 0) {
				delta += 2 * Math.PI;
			} else if (!sweep && delta > 0) {
				delta -= 2 * Math.PI;
			}
			int steps = Math.max(4, (int) Math.ceil(Math.abs(delta) / (Math.PI / CURVE_STEPS)));
			for (int i = 1; i < steps; i++) {
				double angle = start + delta * i / steps;
				double ax = rx * Math.cos(angle);
				double ay = ry * Math.sin(angle);
				lineTo(cos * ax - sin * ay + cx, sin * ax + cos * ay + cy);
			}
			lineTo(toX, toY);
		}

		private int skipSeparators() {
			while (at < d.length() && (Character.isWhitespace(d.charAt(at)) || d.charAt(at) == ',')) {
				at++;
			}
			return at;
		}

		/** The next number, which may run straight on from the one before ("1.1.9" is 1.1 then .9, "0-2" 0 then -2). */
		double number() {
			skipSeparators();
			int start = at;
			if (at < d.length() && (d.charAt(at) == '-' || d.charAt(at) == '+')) {
				at++;
			}
			boolean dot = false;
			while (at < d.length() && (Character.isDigit(d.charAt(at)) || d.charAt(at) == '.' && !dot)) {
				dot |= d.charAt(at) == '.';
				at++;
			}
			if (at < d.length() && (d.charAt(at) == 'e' || d.charAt(at) == 'E')) {
				at++;
				if (at < d.length() && (d.charAt(at) == '-' || d.charAt(at) == '+')) {
					at++;
				}
				while (at < d.length() && Character.isDigit(d.charAt(at))) {
					at++;
				}
			}
			if (start == at) {
				throw new IllegalArgumentException("Expected a number at " + at + " in " + d);
			}
			return Double.parseDouble(d.substring(start, at));
		}

		/** An arc's flag, one character, which may run straight on into the next ("01" is two flags). */
		private boolean flag() {
			skipSeparators();
			char c = at < d.length() ? d.charAt(at++) : ' ';
			if (c != '0' && c != '1') {
				throw new IllegalArgumentException("Expected an arc flag at " + (at - 1) + " in " + d);
			}
			return c == '1';
		}
	}
}
