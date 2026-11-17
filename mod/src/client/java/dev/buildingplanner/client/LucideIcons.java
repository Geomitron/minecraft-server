package dev.buildingplanner.client;

import java.util.List;

/**
 * The radial menu buttons' icons from Lucide (https://lucide.dev), version 1.74.0, copied as each icon's SVG elements
 * from https://github.com/lucide-icons/lucide/tree/1.74.0/icons ({@code <name>.svg}). {@link
 * dev.buildingplanner.look.StrokeIcon} draws them as Lucide's own stylesheet does: stroked 2 wide on a 24-unit square,
 * with round ends and joins. None of these icons is among those Lucide lists as derived from Feather, so Lucide's ISC
 * licence alone covers them:
 *
 * <pre>
 * ISC License
 *
 * Copyright (c) 2026 Lucide Icons and Contributors
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * </pre>
 */
final class LucideIcons {
	/** {@code eye}: ghost blocks show. */
	static final List<String> EYE = List.of(
		"<path d=\"M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0\" />",
		"<circle cx=\"12\" cy=\"12\" r=\"3\" />");
	/** {@code eye-off}: ghost blocks are hidden. */
	static final List<String> EYE_OFF = List.of(
		"<path d=\"M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49\" />",
		"<path d=\"M14.084 14.158a3 3 0 0 1-4.242-4.242\" />",
		"<path d=\"M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143\" />",
		"<path d=\"m2 2 20 20\" />");
	/** {@code copy}: Copy. */
	static final List<String> COPY = List.of(
		"<rect width=\"14\" height=\"14\" x=\"8\" y=\"8\" rx=\"2\" ry=\"2\" />",
		"<path d=\"M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2\" />");
	/** {@code clipboard-paste}: Paste. */
	static final List<String> CLIPBOARD_PASTE = List.of(
		"<path d=\"M11 14h10\" />",
		"<path d=\"M16 4h2a2 2 0 0 1 2 2v1.344\" />",
		"<path d=\"m17 18 4-4-4-4\" />",
		"<path d=\"M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 1.793-1.113\" />",
		"<rect x=\"8\" y=\"2\" width=\"8\" height=\"4\" rx=\"1\" />");
	/** {@code brick-wall}: "Copy real blocks". */
	static final List<String> BRICK_WALL = List.of(
		"<rect width=\"18\" height=\"18\" x=\"3\" y=\"3\" rx=\"2\" />",
		"<path d=\"M12 9v6\" />",
		"<path d=\"M16 15v6\" />",
		"<path d=\"M16 3v6\" />",
		"<path d=\"M3 15h18\" />",
		"<path d=\"M3 9h18\" />",
		"<path d=\"M8 15v6\" />",
		"<path d=\"M8 3v6\" />");
	/** {@code list-checks}: "Material list". */
	static final List<String> LIST_CHECKS = List.of(
		"<path d=\"M13 5h8\" />",
		"<path d=\"M13 12h8\" />",
		"<path d=\"M13 19h8\" />",
		"<path d=\"m3 17 2 2 4-4\" />",
		"<path d=\"m3 7 2 2 4-4\" />");

	private LucideIcons() {
	}
}
