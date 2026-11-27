include(Resources.id("jsblock:scripts/pids_util.js"));

function normalizeStationName(s) {
    return (s == null ? "" : ("" + s))
        .replace(/\|/g, " ")
        .replace(/\s\s+/g, " ")
        .trim();
}

function create(ctx, state, pids) {
}

function render(ctx, state, pids) {
    let scale = 1;
    if (pids.type == "pids_projector") {
        scale = 0.791672;
    }

    let linesLeft = [];
    let linesRight = [];
    let linesColor = [];

    let arrival = pids.arrivals().get(0);
    if (arrival != null) {
        let realtimePrefix = "";
        if (!arrival.realtime() && TextUtil.cycleString(PIDSUtil.getETAText(arrival.arrivalTime())) != "") {
            realtimePrefix = "*";
        }
        linesLeft.push(realtimePrefix + TextUtil.cycleString(PIDSUtil.getETAText(arrival.arrivalTime())));
        if (pids.isPlatformNumberHidden()) {
            linesRight.push("");
        } else {
            let platformNumber = arrival.platformName();
            linesRight.push(TextUtil.cycleString(`${platformNumber}號月台|Plat. ${platformNumber}`));
        }
        linesColor.push(0xFF9900);

        let routeNumber = TextUtil.cycleString(arrival.routeNumber());
        let destinationStr = TextUtil.cycleString(arrival.destination()).trim();
        if(arrival.circularState().toString() == "CLOCKWISE") {
            destinationStr = TextUtil.cycleString("順時針經|Clockwise via ") + destinationStr;
        } else if(arrival.circularState().toString() == "ANTI_CLOCKWISE") {
            destinationStr = TextUtil.cycleString("逆時針經|Anticlockwise via ") + destinationStr;
        }
        linesLeft.push((routeNumber + " " + destinationStr).trim());
        linesRight.push("");
        linesColor.push(0xFF9900);

        linesLeft.push("");
        linesRight.push("");
        linesColor.push(0xFF9900);

        let stationNames = [];
        arrival.route().getPlatforms().toArray().forEach((n) => {
            stationNames.push(n.getStationName());
        });

        // Normalize names before comparing
        let currentStation = pids.station().getName();

        let normalizedCurrent = normalizeStationName(currentStation);
        let currentIndex = -1;

        for (let i = 0; i < stationNames.length; i++) {
            if (normalizeStationName(stationNames[i]) === normalizedCurrent) {
                currentIndex = i;
                break;
            }
        }

        // Remove current station and all previous stations
        if (currentIndex >= 0) {
            stationNames = stationNames.slice(currentIndex + 1);
        }

        let pageCount = Math.ceil(stationNames.length / 10);
        let currentPageCheck = "0";

        for (let i = 1; i < pageCount; i++) {
            currentPageCheck += "|" + i;
        }

        let currentPage = Number(TextUtil.cycleString(currentPageCheck));

        linesLeft.push(`停靠站：(${currentPage + 1}/${pageCount})|Stops at: (${currentPage + 1}/${pageCount})`);
        linesRight.push("");
        linesColor.push(0xFF9900);

        for (let i = 0; i < 10; i++) {
            if (stationNames[i + currentPage * 10]) {
                linesLeft.push(stationNames[i + currentPage * 10]);
            } else {
                linesLeft.push("");
            }
            linesRight.push("");
            linesColor.push(0xFF9900);
        }

        linesLeft.push("");
        linesRight.push("");
        linesColor.push(0xFF9900);

        linesLeft.push("");
        linesRight.push(TextUtil.cycleString(PIDSUtil.getCarText(arrival.carCount())));
        linesColor.push(0xFF0000);
    } else {
        for (let i = 0; i < 16; i++) {
            linesLeft.push("");
            linesRight.push("");
            linesColor.push("");
        }
    }

    for (let i = 0; i < 16; i++) {
        let customMsg = pids.getCustomMessage(i);
        if (customMsg != "") {
            linesLeft[i] = customMsg;
            linesRight[i] = "";
            linesColor[i] = 0xFF9900;
        }
    }
    
    for (let i = 0; i < 16; i++) {
        if (pids.isRowHidden(i)) {
            continue;
        }
        
        if (linesLeft[i] == "" && linesRight[i] == "") {
            continue;
        }

        let textWidth = 33.5;
        if (linesLeft[i] == "" || linesRight[i] == "") {
            textWidth = 72;
        }

        if (linesLeft[i] != "") {
            Text.create()
                .text(TextUtil.cycleString(linesLeft[i]))
                .scale(scale)
                .size(textWidth, 10 * scale)
                .stretchXY()
                .fontMC()
                .pos(5 * scale, 5 * scale + i * (164 / 16 * scale) * 1.04)
                .color(linesColor[i])
                .draw(ctx);
        }

        if (linesRight[i] != "") {
            Text.create()
                .text(TextUtil.cycleString(linesRight[i]))
                .scale(scale)
                .size(textWidth, 10 * scale)
                .stretchXY()
                .fontMC()
                .pos(77 * scale, 5 * scale + i * (164 / 16 * scale) * 1.04)
                .color(linesColor[i])
                .rightAlign()
                .draw(ctx);
        }
    }
}

function dispose(ctx, state, pids) {
}