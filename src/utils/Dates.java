package utils;

import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Dates
{
//      * Choix de la langue francaise
        static Locale locale = Locale.getDefault();
        static Date actuelle = new Date();

//      * Definition du format utilise pour les dates
        static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh'h'mm");

//      * Donne la date au format "aaaa-mm-jj"
        public static String date()
        {
                String dat = dateFormat.format(actuelle);
                return dat;
        }
}
