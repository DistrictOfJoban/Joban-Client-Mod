const PIDSUtil = {
    getETAText(arrival, arrivedText) {
        let willArriveIn = arrival - Date.now();
        let willArriveInSec = Math.floor(willArriveIn/1000);
        if(willArriveInSec <= 0) {
            return arrivedText ? arrivedText : "";
        } else if(willArriveIn <= 60000) {
            return `${willArriveInSec} 秒|${willArriveInSec} sec`;
        } else if(willArriveIn <= 120000) { // Less than 2 mins, use non-plural form
            return `${Math.floor(willArriveInSec/60)} 分鐘|${Math.floor(willArriveInSec/60)} min`;
        } else {
             return `${Math.floor(willArriveInSec/60)} 分鐘|${Math.floor(willArriveInSec/60)} mins`;
         }
    },
    formatTime(inGameTime, padZero) {
        let timeNow = inGameTime + 6000;
        let hrs = (timeNow / 1000) % 24;
        let mins = (hrs - Math.floor(hrs)) * 60;

        if(padZero) {
            return `${Math.floor(hrs).toString().padStart(2, '0')}:${(Math.floor(mins % 60)).toString().padStart(2, '0')}`;
        } else {
            return `${Math.floor(hrs).toString()}:${(Math.floor(mins % 60)).toString().toString().padStart(2, '0')}`;
        }
    },
    getCarText(len) {
        if(len <= 1) {
            return `${len}卡|${len}-car`;
        } else {
            return `${len}卡|${len}-cars`;
        }
    },
    formatDateTime(d) {
        return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
    }
}