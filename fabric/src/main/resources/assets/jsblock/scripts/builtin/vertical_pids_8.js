include(Resources.id("jsblock:scripts/pids_util.js"));

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

            let realtimePrefix = "";
            if (!arrival.realtime() && TextUtil.cycleString(PIDSUtil.getETAText(arrival.arrivalTime())) != "") {
                realtimePrefix = "*";
            }
            let etaText = realtimePrefix + TextUtil.cycleString(PIDSUtil.getETAText(arrival.arrivalTime()));
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