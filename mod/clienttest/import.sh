# The import's in-game check: the dev server, started without Syncmatica on a world with plans Syncmatica shared,
# moves them into the layer once, logs it, and serves the same blocks, also after a restart. The world is the unit
# test's fixture (PlanImportTest writes it to build/import-fixture/), and the expected blocks are where Litematica's own
# code places them. Planner1's driver asks for the columns with protocol 3's watch, which the client doesn't speak yet.
# Sourced by inside.sh, which provides drv, check, start_server, stop_server, start_client and stop_client.

stop_client 2
stop_client 1
stop_server

echo "Writing the import's fixture..."
if ! ./gradlew --no-daemon -q test --tests dev.buildingplanner.server.PlanImportTest > /out/fixture.log 2>&1; then
	tail -40 /out/fixture.log
	exit 1
fi
# The first start made an empty layer; the import runs only on a world without one.
rm -rf run/server/world/building-planner
cp -r build/import-fixture/world/. run/server/world/
cp -r build/import-fixture/server/syncmatics run/server/
cp build/import-fixture/expected.txt run/client1/import-expected.txt
echo "  $(wc -l < run/client1/import-expected.txt) overworld ghost blocks expected" | tee -a /out/checks.log

start_server "" server-import
check "the server runs without Syncmatica" eval '! grep -qE "^\s+- syncmatica " /out/server-import.log'
check "the server logs the import's counts" \
	grep -qF "Building Planner moved 4 of 6 shared plans into the layer" /out/server-import.log
grep -F "Building Planner moved" /out/server-import.log | sed 's/^/  /' >> /out/checks.log
check "it logs the two plans it couldn't read" \
	eval '[[ $(grep -c "Building Planner couldn.t read plan" /out/server-import.log) == 2 ]]'
check "it wrote the layer and left Syncmatica's files" eval '[[ -d run/server/world/building-planner/layer/minecraft/overworld \
	&& -f run/server/world/syncmatica/placements.json && ! -e run/server/world/building-planner/layer-import ]]'

# serves <log>: Planner1 joins, stands at the plans and gets every expected block, and no other, in the columns.
serves() {
	start_client 1
	timeout 300 grep -q -m1 -F "Planner1 joined the game" <(tail -n +1 -F "/out/$1.log") \
		&& drv 1 "wait 100" "cmd tp @s 0 -60 0" "wait 40" "layerwatch import-expected.txt" \
			"await layer import-expected.txt"
}
check "the server serves the imported blocks" serves server-import

# Stopped and started again, the server reads the layer back and doesn't import again.
stop_client 1
stop_server server-import
start_server "" server-restart
check "a second start doesn't import again" eval '! grep -qF "Building Planner moved" /out/server-restart.log'
check "and serves the same blocks" serves server-restart
