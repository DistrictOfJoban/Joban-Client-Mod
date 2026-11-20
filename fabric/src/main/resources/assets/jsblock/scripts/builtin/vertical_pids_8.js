include(Resources.id("jsblock:scripts/pids_util.js"));

function create(ctx, state, pids) {
}

function render(ctx, state, pids) {
    let linesLeft = [];
    let linesRight = [];
    let linesColor = [];

    for (let i = 0; i < 8; i++) {
        let arrival = pids.arrivals().get(i);
        if (arrival != null) {
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

            let etaText = TextUtil.cycleString(PIDSUtil.getETAText(arrival.arrivalTime()));
            let carText = TextUtil.cycleString(PIDSUtil.getCarText(arrival.carCount()));
            
            let etaOrCarText = etaText;
            let needShowCarText = pids.arrivals().mixedCarLength();
            if(needShowCarText) etaOrCarText += `|${carText}`;
            
            let switchDuration = needShowCarText ? 120 : 60;

            linesLeft.push("");
            linesRight.push(TextUtil.cycleString(etaOrCarText, switchDuration));
            linesColor.push(0xFF9900);
        } else {
            linesLeft.push("");
            linesRight.push("");
            linesColor.push(0xFF9900);
            linesLeft.push("");
            linesRight.push("");
            linesColor.push(0xFF9900);
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