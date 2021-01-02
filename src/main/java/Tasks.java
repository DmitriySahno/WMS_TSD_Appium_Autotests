import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class Tasks {

    private static final AndroidDriver<MobileElement> ad = Automation.driver; //get driver

    private enum COMMIT_ERRORS {  //errors, returned with committing tasks
        EXPIRATION_DATE_CONTAINER_DISCREPANCY, //discrepancy of expiration percent of goods at container
        CRITICAL_EXPIRATION_DATE   //discrepancy of expiration date by expiration percent
    }

    public static List getTaskList() {
        //List<MobileElement> list = ad.findElementsById("com.abmcloud:id/card_view");
        //AndroidDriver<MobileElement> ad = Automation.driver; //get driver
        List<MobileElement> list = ad.findElementsById("com.abmcloud:id/text_work_type");
        List<String> resultList = new ArrayList<String>();
        for (MobileElement me : list) {
            resultList.add(me.getText());
        }
        return resultList;
    }

    //selecting task by task name
    public static int choseTask (String taskName) throws IOException {
        //AndroidDriver<MobileElement> ad = Automation.driver; //get driver
        WebDriverWait wait = new WebDriverWait(ad, 5);
        WebElement tasks = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/swipe_container")));

        MobileElement meTaskList = ad.findElementById("com.abmcloud:id/recycler_view");
        List<MobileElement> list = ad.findElementsById("com.abmcloud:id/card_view");

        Automation.writeLog("Selecting task: " + taskName);

        int taskQty = 0;
        for ( MobileElement element : meTaskList.findElementsById("com.abmcloud:id/card_view")) {
            if (taskName.equals(element.findElementById("com.abmcloud:id/text_work_type").getText())) {
                taskQty = Integer.parseInt(element.findElementById("com.abmcloud:id/text_qty").getText());
                element.findElementById("com.abmcloud:id/text_qty").click();
                Automation.writeLog("Task " + taskName + " is selected");
                break;
            }
        }

        return taskQty;
    }

    //processing container operation
    public static void performContainerTask() {
        //get information about tasks
        //AndroidDriver<MobileElement> ad = Automation.driver; //get driver
        Wait wait = new WebDriverWait(ad, 20);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/editTextHeaderContainerMoving")));

        String srcField = "com.abmcloud:id/editTextBoxBarcodeContainerMoving";
        String srcFieldDescr = "com.abmcloud:id/textViewHintSourceContainerMoving";
        String destField = "com.abmcloud:id/editTextDestinationContainerMoving";
        String destFieldDescr = "com.abmcloud:id/textViewHintDestinationContainerMoving";

        String taskInfo = ad.findElementById("com.abmcloud:id/editTextHeaderContainerMoving").getAttribute("text");
        //get count of tasks
        int taskCount = Integer.parseInt(taskInfo.substring(taskInfo.indexOf(":", taskInfo.indexOf("["))+1, taskInfo.indexOf(" ", taskInfo.indexOf("["))));
        //complete all containers in the task
        for (int i = 1; i <= taskCount ; i++) {
            //WebDriverWait wait = new WebDriverWait(ad, 10);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(srcFieldDescr)));
            String textSource = ad.findElementById(srcFieldDescr).getText();

            if (ad.findElementById(srcField).getText().equals("")) { //if container is clear
                if (textSource.contains("/")) {
                    ad.findElementById(srcField).
                            sendKeys(textSource.substring(textSource.indexOf("/") + 1));
                    ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                    //checking wrong cell entering error
                    checkErrorMsg(ad);
                }
            }

            if (ad.findElementById(destField).getText().equals("")) {
                ad.findElementById(destField).sendKeys(ad.findElementById(destFieldDescr).getText());
                ad.pressKey(new KeyEvent(AndroidKey.ENTER));
            }
            ad.findElementById("com.abmcloud:id/buttonCommitContainerMoving").click();
            //checking print cargo message
            checkCargoPrintMsg(ad);
            //checking wrong cell entering error
            checkErrorMsg(ad);

            //TODO checking task changing
            if (!waitCommit(srcField, destField))
                return;
        }
    }

    //processing picking operation
    public static void performPickingTask () {
        //AndroidDriver<MobileElement> ad = Automation.driver; //get driver
        WebDriverWait wait = new WebDriverWait(ad, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/source_edit")));

        //get information about tasks
        String taskInfo = ad.findElementById("com.abmcloud:id/collapsing_toolbar").getAttribute("content-desc");

        String srcField = "com.abmcloud:id/source_edit";
        String productField = "com.abmcloud:id/ware_barcode";
        String productDescr = "com.abmcloud:id/textViewDescription";
        String destField = "com.abmcloud:id/dest_edit";
        String qtyField = "com.abmcloud:id/editTextQty";
        String qtyDescr = "com.abmcloud:id/textViewLabelHint";

        //get count of tasks
        //int taskCount = Integer.parseInt(taskInfo.substring(taskInfo.indexOf(":", taskInfo.indexOf("["))+1, taskInfo.indexOf(" ", taskInfo.indexOf("["))));
        //for (int i = 1; i <= taskCount ; i++) {

        while (true) {
            enterSource(srcField);

            enterProduct(productField, productDescr);

            enterDestination(destField);

            enterQuantity(qtyField, qtyDescr);

            ad.findElementById("com.abmcloud:id/buttonCommit").click();

//            while (ad.findElementsById(srcField).size()<0) { //just wait to activate task form
//                if (ad.findElementsById("com.abmcloud:id/swipe_container").size()>0)
//                    return;
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

            if (!waitCommit(srcField, productField, destField, qtyField))
                return;

            //}
        }

    }

    //processing receiving operation
    public static void performReceivingTask() {
        Wait wait = new WebDriverWait(ad, 20);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/editTextControlSource")));
        StringBuffer customExpDate = new StringBuffer("");
        //get information about tasks
        String taskInfo = ad.findElementById("com.abmcloud:id/collapsing_toolbar").getAttribute("content-desc");
        //get count of tasks
        //int taskCount = Integer.parseInt(taskInfo.substring(taskInfo.indexOf(":", taskInfo.indexOf("["))+1, taskInfo.indexOf(" ", taskInfo.indexOf("["))));

        while (true) {

            String srcField = "com.abmcloud:id/editTextControlSource";
            String productField = "com.abmcloud:id/editTextControlBoxBarcode";
            String productDescr = "com.abmcloud:id/textViewControlDescription";
            String destField = "com.abmcloud:id/editTextControlDestination";
            String qtyField = "com.abmcloud:id/editTextControlQty";
            String qtyDescr = "com.abmcloud:id/labelHintControl";

            float count = Float.parseFloat(ad.findElementById(qtyDescr).getText()); // get planned qty to use it at entering SN

            enterSource(srcField);

            enterProduct(productField, productDescr);

            enterSerialNumbers(count);

            checkBatchAndExpDate(customExpDate);

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            enterQuantity(qtyField, qtyDescr);

            enterDestination(destField);

            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ad.findElementById("com.abmcloud:id/buttonControlCommit").click();

            Map<COMMIT_ERRORS, String> resultMap = checkToREReceiving();

            if (resultMap!=null)
            for (Map.Entry<COMMIT_ERRORS, String> e : resultMap.entrySet()) {
                switch (e.getKey()) {
                    case CRITICAL_EXPIRATION_DATE:
                        customExpDate.delete(0, customExpDate.length());
                        customExpDate.append(e.getValue());
                        ad.findElementById(productField).clear(); //clear product
                        ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                        //product.findElementById("com.abmcloud:id/text_input_end_icon").click(); // clear by button click
                        //i--; //decrement, task isn`t complete
                        continue;
                    case EXPIRATION_DATE_CONTAINER_DISCREPANCY:
                        ad.findElementById(destField).clear(); //clear destination field
                        //i--; //decrement, task isn`t complete
                        continue;
                    default:
                        break;
                }
            }

            if (!waitCommit(srcField, productField, destField, qtyField))
                return;
        }
    }

    //processing placing & moving operation
    public static void performMovingTask() {
        Wait wait = new WebDriverWait(ad, 20);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/source_edit")));
        //get information about tasks
        String taskInfo = ad.findElementById("com.abmcloud:id/collapsing_toolbar").getAttribute("content-desc");
        //get count of tasks
        int taskCount = Integer.parseInt(taskInfo.substring(taskInfo.indexOf(":", taskInfo.indexOf("["))+1, taskInfo.indexOf(" ", taskInfo.indexOf("["))));

        for (int i = 1; i <= taskCount ; i++) {

            String srcField = "com.abmcloud:id/source_edit";
            String productField = "com.abmcloud:id/ware_barcode";
            String productDescr = "com.abmcloud:id/textViewDescription";
            String destField = "com.abmcloud:id/dest_edit";
            String qtyField = "com.abmcloud:id/editTextQty";
            String qtyDescr = "com.abmcloud:id/textViewLabelHint";

            enterSource(srcField);

            enterProduct(productField, productDescr);

            enterDestination(destField);

            enterQuantity(qtyField, qtyDescr);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ad.findElementById("com.abmcloud:id/buttonCommit").click();

//            if (i<taskCount) { // if current iteration less then count of task, wait cleaning product field
            if (!waitCommit(srcField, productField, destField, qtyField))
                return;
//            }
        }
    }

    private static void checkBatchAndExpDate(StringBuffer customExpDate) {
        boolean batches = ad.findElements(By.id("com.abmcloud:id/layoutSerial")).size()>0;
        boolean expDates = ad.findElements(By.id("com.abmcloud:id/layoutShelfLife")).size()>0;

        //check batch and fill it
        if (batches) {
            //DON`T DELETE, WILL USE IN FURTHER
            //          List<MobileElement> availableExpDates = ad.findElementById("com.abmcloud:id/listViewItemsLP").
            //                 findElements(By.className("android.view.ViewGroup"));
            //TODO remake to random checking and foresee clear list
            //availableExpDates.get(0).click();
            MobileElement series = ad.findElementById("com.abmcloud:id/textBoxSeriesPL");
            SimpleDateFormat format = new SimpleDateFormat("MMyyyy");
            Calendar currentDate = Calendar.getInstance();
            currentDate.add(Calendar.DAY_OF_MONTH, 7); // adding a week to current date
            series.sendKeys("AA" + format.format(currentDate.getTime()));
        }

        //check expiration date and fill it
        if (expDates) {
            //DON`T DELETE, WILL USE IN FURTHER
//                List<MobileElement> availableExpDates = ad.findElementById("com.abmcloud:id/listViewItemsLP").
//                        findElements(By.className("android.view.ViewGroup"));
//                //TODO remake to random checking and foresee clear list
//                availableExpDates.get(0).click();
            MobileElement expDate = ad.findElementById("com.abmcloud:id/textBoxShelfLifeLP");
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            if (customExpDate.toString().equals("")) {
                Calendar currentDate = Calendar.getInstance();
                currentDate.add(Calendar.DAY_OF_MONTH, 60); // adding a week to current date
                expDate.sendKeys(format.format(currentDate.getTime()));
            } else {
                expDate.sendKeys(customExpDate);
                customExpDate.delete(0,customExpDate.length()); // clear linked variable
            }

        }

        if (batches||expDates) {
            ad.findElementById("android:id/button1").click(); //click OK button

            /*
            //Temporary while bag with doubled window will be fixed
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (ad.findElementsById("android:id/button1").size() > 0) {
                ad.findElementById("android:id/button1").click();
            }
            */
        }

    }

    private static Map<COMMIT_ERRORS, String> checkToREReceiving() {

        MobileElement message = null;
        for (int j = 0; j < 5 ; j++) {
            if (ad.findElementsById("android:id/message").size()>0) {
                message = ad.findElementById("android:id/message");
                break;
            }

            if (j==4) {
                return null;
            }
        }


        Map<COMMIT_ERRORS, String> resultMap = new HashMap<>();


        if (message.getText().contains("невозможно положить товар со сроком годности")) {
            /* EXAMPLE
            В контейнер KOR1752 невозможно положить товар со сроком годности .
            Процент отклонения от сроков годности товара в контейнере больше, чем 0%
            */
            resultMap.put(COMMIT_ERRORS.EXPIRATION_DATE_CONTAINER_DISCREPANCY,null);
//            ad.findElementById("android:id/button2").click(); // commit error
//            return resultMap;

        }
        if (message.getText().contains("Ограничение по сроку годности!")) {
            /* EXAMPLE
            Ограничение по сроку годности! У номенклатуры Віскі Тічерз 0.7л срок действия 16.12.2020 остаток срока годности 6,01%.
            Допускается не ниже10%.
            */
            String text = message.getText();
            Calendar usedExpDate = Calendar.getInstance();
            //parse initial data
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            int expDateIdx = text.indexOf("срок действия")+"срок действия".length()+1;
            try {
                usedExpDate.setTime(format.parse(text.substring(expDateIdx,expDateIdx+10)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            int factExpPercIdx= text.indexOf("срока годности")+"срока годности".length()+1;
            float factExpiration = Float.parseFloat(text.substring(factExpPercIdx, text.indexOf("%",factExpPercIdx)).replace(",","."));
            //int borderExpPercIdx = text.indexOf("ниже")+"ниже".length();
            //float allowExpiration = Float.parseFloat(text.substring(borderExpPercIdx, text.indexOf("%",borderExpPercIdx)).replace(",","."));

            //calculating new expiration date with expiration 90%
            Calendar todayDate = Calendar.getInstance();
            long currentDifferenceDays = ChronoUnit.DAYS.between(todayDate.toInstant(), usedExpDate.toInstant());
            int shelfLife = (int) (currentDifferenceDays*100/factExpiration);
            int planDifferenceDays = (int) (shelfLife-shelfLife*0.1); //10 - new expiration percent
            Calendar newExpDate = Calendar.getInstance();
            newExpDate.add(Calendar.DAY_OF_YEAR, planDifferenceDays);
            resultMap.put(COMMIT_ERRORS.CRITICAL_EXPIRATION_DATE, format.format(newExpDate.getTime()));
//            ad.findElementById("android:id/button2").click(); // commit error
//            return resultMap;
        }
        if (message.getText().contains("Расчетное значение даты производства больше текущей даты!")) {
            /*
            Расчетное значение даты производства больше текущей даты!
            Проверьте количество дней хранения (текущее значение = 10 дней) или срок годности!
             */
            String text = message.getText();
            int shelfLife = Integer.parseInt(text.substring(text.indexOf("=")+2,text.indexOf(" дней)")));
            Calendar todayDate = Calendar.getInstance();
            todayDate.add(Calendar.DAY_OF_YEAR, (int) (shelfLife*0.9)); //get rest life 90%
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            resultMap.put(COMMIT_ERRORS.CRITICAL_EXPIRATION_DATE, format.format(todayDate.getTime()));
//            ad.findElementById("android:id/button2").click(); // commit error
//            return resultMap;
        }

        ad.findElementById("android:id/button2").click(); // commit error
        return resultMap;
    }

    private static void fillGoodInfo() {
        try {
            Wait wait = new WebDriverWait(ad, 2);
            MobileElement title = (MobileElement) wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/alertTitle")));
            if (title.getText().equals("Введите ВГХ")) {
                MobileElement height = ad.findElementById("com.abmcloud:id/textBoxHeightWP");
                if (height.getText().equals("0")) {
                    height.clear();
                    height.sendKeys("0.1");
                    ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                }
                MobileElement width = ad.findElementById("com.abmcloud:id/textBoxWidthWP");
                if (width.getText().equals("0")) {
                    width.clear();
                    width.sendKeys("0.2");
                    ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                }
                MobileElement length = ad.findElementById("com.abmcloud:id/textBoxLengthWP");
                if (length.getText().equals("0")) {
                    length.clear();
                    length.sendKeys("0.3");
                    ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                }
                MobileElement weight = ad.findElementById("com.abmcloud:id/textBoxWeightWP");
                if (weight.getText().equals("0")) {
                    weight.clear();
                    weight.sendKeys("1.3");
                    ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                }
                MobileElement shelfLife = ad.findElementById("com.abmcloud:id/textBoxStoragePeriod");
                if (shelfLife.getText().equals("0")) {
                    shelfLife.clear();
                    shelfLife.sendKeys("180");
                    ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                }
                ad.findElementById("android:id/button1").click(); // click OK button
            }

        } catch (Exception e){}
    }

    private static void enterSource (String rid) {
        MobileElement src = ad.findElementById(rid);
        //if (srcText.equals("НЕТ"))  //need to process cell НЕТ by fix PS
//        if (!src.getText().contains(",")) { // if source is clear
        if (src.getText().toLowerCase().contains("источник")) { // it means the source is clear
            src.sendKeys(getSource(src.getText()));
            ad.pressKey(new KeyEvent(AndroidKey.ENTER));
            // ! need to check errors
        }
//        if (src.getText().equals("")) { // if source is clear
////            src.sendKeys(getSource(src.getText()));
////            ad.pressKey(new KeyEvent(AndroidKey.ENTER));
////            // ! need to check errors
////        }

    }

    private static void enterProduct (String rid, String descrid) {
        //PRODUCT
        MobileElement product = ad.findElementById(rid);
        //product.clear(); //temporary
        if (product.getText().toLowerCase().equals("товар")) { //if empty
            //get description and parse product code
            MobileElement descr = ad.findElementById(descrid);
            String descrText = descr.getText();
            int prodCodeBeginIdx = descrText.indexOf("(") == 0 ? descrText.indexOf(")") : -1; // if descr contains info before product code like "(2/2)", check, if conains ( at first place
            prodCodeBeginIdx = (prodCodeBeginIdx < 0) ? 0 : prodCodeBeginIdx + 2;
            //if return to next string after space
            String prodCode = (descrText.indexOf(" ", prodCodeBeginIdx) < descrText.indexOf("\n", prodCodeBeginIdx)) ?
                    descrText.substring(prodCodeBeginIdx, descrText.indexOf(" ")) :
                    descrText.substring(prodCodeBeginIdx, descrText.indexOf("\n"));
            //enter product code
            product.sendKeys(prodCode);
            ad.pressKey(new KeyEvent(AndroidKey.ENTER));
            fillGoodInfo();
        }
    }

    private static void enterDestination(String rid) {
        //DESTINATION
        MobileElement dest = ad.findElementById(rid);
        String destText = dest.getText();

        if (!dest.isDisplayed()) {
            return;
        }

        if (destText.toLowerCase().replace("контейнер", "").equals("")||destText.toLowerCase().contains("назначение")) { //if field is clear
            if (destText.toLowerCase().equals("контейнер")||destText.toLowerCase().equals("назначение: контейнер")) { //and destination is a container
                dest.sendKeys(getContainer()); //get new container
            } else {
                dest.sendKeys(destText.replace("Назначение: ", "")); //if destination isn`t container, enter destination from description
            }
            ad.pressKey(new KeyEvent(AndroidKey.ENTER));
        }
    }

    private static void enterQuantity(String rid, String descrid) {
        //QUANTITY
        MobileElement qty = ad.findElementById(rid);
        if (qty.getText().toLowerCase().equals("количество")) { //if not serial number mode and empty
            MobileElement planQty = ad.findElementById(descrid);
            qty.sendKeys(planQty.getText());
            ad.pressKey(new KeyEvent(AndroidKey.ENTER));
        }
    }

    private static boolean enterSerialNumbers(float count) {
        try {
            Wait wait = new WebDriverWait(ad, 3);
            MobileElement serialNumber = (MobileElement) wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/editTextSerialNumber")));
            SimpleDateFormat format = new SimpleDateFormat("ddMMyyyyHHmmsss");
            for (int i = 0; i < count; i++) {
                Calendar calendar = Calendar.getInstance();
                serialNumber.sendKeys(format.format(calendar.getTime()));
                ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (ad.findElements(By.id("android:id/message")).size()>0) {  //check, if message exist, accept it and exit from cycle
                    ad.findElementById("android:id/button2").click();
                    break;
                }
            }
            ad.findElementById("com.abmcloud:id/buttonSNCommit").click();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //processing receiving operation
    public static void performPlacingTaskOLD() {
        Wait wait = new WebDriverWait(ad, 20);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/source_edit")));
        //get information about tasks
        String taskInfo = ad.findElementById("com.abmcloud:id/collapsing_toolbar").getAttribute("content-desc");
        //get count of tasks
        int taskCount = Integer.parseInt(taskInfo.substring(taskInfo.indexOf(":", taskInfo.indexOf("["))+1, taskInfo.indexOf(" ", taskInfo.indexOf("["))));

        for (int i = 1; i <= taskCount ; i++) {
            //SOURCE
            MobileElement src = ad.findElementById("com.abmcloud:id/source_edit");
            String srcText = getSource(src.getText());
            //if (srcText.equals("НЕТ"))  //need to process cell НЕТ by fix PS
            if (srcText.indexOf(",")<0) { // if source is clear
                src.sendKeys(srcText);
                ad.pressKey(new KeyEvent(AndroidKey.ENTER));
                // ! need to check errors
            }

            //PRODUCT
            MobileElement product = ad.findElementById("com.abmcloud:id/ware_barcode");
            product.clear();
            if (product.getText().indexOf(",")<0) { //if empty
                //get description and parse product code
                MobileElement descr = ad.findElementById("com.abmcloud:id/textViewDescription");
                String descrText = descr.getText();
                int prodCodeBeginIdx = descrText.indexOf("(") == 0 ? descrText.indexOf(")") : -1; // if descr contains info before product code like "(2/2)", check, if conains ( at first place
                prodCodeBeginIdx = (prodCodeBeginIdx < 0) ? 0 : prodCodeBeginIdx + 2;
                //if return to next string after space
                String prodCode = (descrText.indexOf(" ", prodCodeBeginIdx) < descrText.indexOf("\n", prodCodeBeginIdx)) ?
                        descrText.substring(prodCodeBeginIdx, descrText.indexOf(" ")) :
                        descrText.substring(prodCodeBeginIdx, descrText.indexOf("\n"));
                //enter product code
                product.sendKeys(prodCode);
                ad.pressKey(new KeyEvent(AndroidKey.ENTER));
            }

            //QUANTITY
            MobileElement qty = ad.findElementById("com.abmcloud:id/editTextQty");
            if (!qty.getText().contains(",")) { //if not serial number mode and empty
                MobileElement planQty = ad.findElementById("com.abmcloud:id/textViewLabelHint");
                qty.sendKeys(planQty.getText());
                ad.pressKey(new KeyEvent(AndroidKey.ENTER));
            }

            //DESTINATION
            MobileElement dest = ad.findElementById("com.abmcloud:id/dest_edit");
            String destText = dest.getText().replace("Назначение: ", "");
            if (dest.isDisplayed()&&!destText.contains(",")) { //if container displayed & destination is clear (some trouble with element text, because edit text contains description)
                if (destText.toLowerCase().equals("контейнер")) { //and destination is a container
                    dest.sendKeys(getContainer()); //get new container
                } else {
                    dest.sendKeys(destText); //if destination isn`t container, enter destination from description
                }
                ad.pressKey(new KeyEvent(AndroidKey.ENTER));
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ad.findElementById("com.abmcloud:id/buttonCommit").click();

            if (i<taskCount) { // if current iteration less then count of task, wait cleaning product field
                while (product.getText().contains(",")&&qty.getText().contains(",")&&src.getText().contains(",")&&dest.getText().contains(",")) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }


        }


    }

    private static boolean waitCommit(String...rids) {
        while (true) {
            for (String s : rids) {
                if (ad.findElementsById(s).size()==0) { //if element doesn`t exist
                    return false;
                }
                String text = ad.findElementById(s).getText().toLowerCase();
                if (text.equals("")||
                    text.equals("товар")||
                    text.equals("контейнер")||
                    text.equals("назначение: контейнер")||
                    text.contains("источник: ")) {
                    return true;
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //checking wrong cell entering error
    private static void checkErrorMsg(AndroidDriver ad) {
        try {
            WebDriverWait wait = new WebDriverWait(ad, 5);
            WebElement meTextMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("android:id/message")));
            Automation.writeLog("Error message: " + meTextMessage.getText());
        } catch (RuntimeException e) {}
    }

    private static void checkCargoPrintMsg(AndroidDriver ad) {
        try {
            WebDriverWait wait = new WebDriverWait(ad, 10);
            WebElement meTitleMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/alertTitle")));
            Automation.writeLog("Get message: "+meTitleMsg.getText());
            Automation.writeLog("Write cargo labels count to print: 2");
            ad.findElementById("com.abmcloud:id/prompt").sendKeys("2");
//            MobileElement meTitleMsg = ad.findElementById("com.abmcloud:id/alertTitle");
            //Need to change copies count
            ad.findElementById("android:id/button1").click();
        } catch (Exception e) {}
    }

    private static String getContainer() {
        String containerName = "";
        try {

            BufferedReader reader = new BufferedReader(new FileReader(Automation.containers));
            containerName = reader.readLine();
            List<String> containers = new ArrayList<String>();
            while (reader.ready()) {
                containers.add(reader.readLine());
            }
            reader.close();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Automation.containers)));
            for (String s: containers) {
                writer.write(s);
                writer.newLine();
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return containerName;
    }

    private static String getSource(String text) {
        text = text.replace( "Источник: " , "");
        if (text.contains("/")) {
            //get container name
            return text.substring(text.indexOf("/")+1);
        } else return text;
    }
}
