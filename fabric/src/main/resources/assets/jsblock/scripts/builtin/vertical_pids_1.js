include(Resources.id("jsblock:scripts/pids_util.js"));

function create(ctx, state, pids) {
}

function render(ctx, state, pids) {
    let linesLeft = [];
    let linesRight = [];
    let linesColor = [];

    let arrival = pids.arrivals().get(0);
    if (arrival != null) {
        linesLeft.push(TextUtil.cycleString(PIDSUtil.getETAText(arrival.arrivalTime())));
        if (pids.isPlatformNumberHidden) {
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
            stationNames.push(n);
        });
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

        let textWidth = 35.5;
        if (linesLeft[i] == "" || linesRight[i] == "") {
            textWidth = 76;
        }

        if (linesLeft[i] != "") {
            Text.create()
                .text(linesLeft[i])
                .scale(1.725)
                .size(textWidth, 10)
                .stretchXY()
                .fontMC()
                .pos(5, 5 + i * 11)
                .color(linesColor[i])
                .draw(ctx);
        }

        if (linesRight[i] != "") {
            Text.create()
                .text(linesRight[i])
                .scale(1.725)
                .size(textWidth, 10)
                .stretchXY()
                .fontMC()
                .pos(5, 5 + i * 11)
                .color(linesColor[i])
                .rightAlign()
                .draw(ctx);
        }
    }
}

function dispose(ctx, state, pids) {
}