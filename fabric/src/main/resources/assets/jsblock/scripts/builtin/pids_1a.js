include(Resources.id("jsblock:scripts/pids_util.js"));
const TOP_PADDING = 5;
const SIDE_PADDING = 3;
const MAX_ARRIVALS = 3;

function create(ctx, state, pids) {
}

function render(ctx, state, pids) {
    let arrivalIdx = 0;
    let perRowHeight = (pids.height - TOP_PADDING) / MAX_ARRIVALS;
    
    for(let i = 0; i < MAX_ARRIVALS; i++) {
        let rowY = TOP_PADDING + (i*perRowHeight);
        let customMsg = pids.getCustomMessage(i);
        let arrival = pids.arrivals().get(arrivalIdx);
        
        if(customMsg != "") {
            Text.create("Custom Text")
            .text(TextUtil.cycleString(customMsg))
            .scale(1.725)
            .size((pids.width / 1.725) - ((SIDE_PADDING / 1.725) * 2), 9)
            .stretchXY()
            .fontMC()
            .color(0xFC9700)
            .pos(SIDE_PADDING, rowY)
            .draw(ctx);
        } else if(arrival != null && !pids.isRowHidden(i)) {
            let routeNumber = TextUtil.cycleString(arrival.routeNumber());
            let destinationStr = TextUtil.cycleString(arrival.destination()).trim();

            if(arrival.circularState().toString() == "CLOCKWISE") {
                destinationStr = TextUtil.cycleString("順時針經|Clockwise via ") + destinationStr;
            } else if(arrival.circularState().toString() == "ANTI_CLOCKWISE") {
                destinationStr = TextUtil.cycleString("逆時針經|Anticlockwise via ") + destinationStr;
            }

            let finalDestinationDisplay = (routeNumber + " " + destinationStr).trim();

            Text.create("Destination Text")
            .text(finalDestinationDisplay)
            .scale(1.725)
            .size((pids.width / 1.725) - 30 - ((SIDE_PADDING / 1.725) * 3), 9)
            .stretchXY()
            .fontMC()
            .color(0xFC9700)
            .pos(SIDE_PADDING, rowY)
            .draw(ctx);
            
            let etaText = TextUtil.cycleString(PIDSUtil.getETAText(arrival.arrivalTime()));
            let carText = TextUtil.cycleString(PIDSUtil.getCarText(arrival.carCount()));
            
            let etaOrCarText = etaText;
            let needShowCarText = pids.arrivals().mixedCarLength();
            if(needShowCarText) etaOrCarText += `|${carText}`;
            
            let switchDuration = needShowCarText ? 120 : 60;
            
            Text.create("ETA Text")
            .text(TextUtil.cycleString(etaOrCarText, switchDuration))
            .scale(1.725)
            .size(30, 9)
            .stretchXY()
            .rightAlign()
            .fontMC()
            .color(0xFC9700)
            .pos(pids.width - SIDE_PADDING, rowY)
            .draw(ctx);
            arrivalIdx++;
        }
    }
}

function dispose(ctx, state, pids) {
}