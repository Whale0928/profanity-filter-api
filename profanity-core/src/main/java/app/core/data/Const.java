package app.core.data;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class Const {
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public static int getCurrentYear() {
        return getCurrentDateTime().getYear();
    }

    public static int getCurrentMonth() {
        return getCurrentDateTime().getMonthValue();
    }

    public static int getCurrentDay() {
        return getCurrentDateTime().getDayOfMonth();
    }

    public static String getCurrentDate() {
        return String.format("%04d-%02d-%02d", getCurrentYear(), getCurrentMonth(), getCurrentDay());
    }

}
