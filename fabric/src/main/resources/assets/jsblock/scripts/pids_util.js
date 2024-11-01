const PIDSUtil = {
    getETAText(arrival) {
        let willArriveIn = arrival - Date.now();
        let willArriveInSec = Math.floor(willArriveIn/1000);
        if(willArriveInSec <= 0) {
            return "";
        } else if(willArriveIn <= 60000) {
            return `${willArriveInSec} 秒|${willArriveInSec} sec`;
        } else {
            return `${Math.floor(willArriveInSec/60)} 分鐘|${Math.floor(willArriveInSec/60)} min`;
        }
    },
    formatTime(inGameTime) {
        let timeNow = inGameTime + 6000;
        let hrs = (timeNow / 1000) % 24;
        let mins = (hrs - Math.floor(hrs)) * 60;

        return `${Math.floor(hrs).toString().padStart(2, '0')}:${(Math.floor(mins % 60)).toString().padStart(2, '0')}`;
    }
}