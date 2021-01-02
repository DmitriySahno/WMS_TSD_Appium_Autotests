import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


public class Automation {
    private static FileWriter log;
    public static final File containers = new File("src/main/resources/containers.txt");
    public static AndroidDriver<MobileElement> driver;

    static {
        //create log file
        try {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HHmmss", Locale.ENGLISH);
            File file = new File("src\\log\\log_"+format.format(calendar.getTime())+".txt");
            file.createNewFile();
            log = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @BeforeTest
    public static void setup() throws MalformedURLException {
        DesiredCapabilities dc = new DesiredCapabilities();
        dc.setCapability(MobileCapabilityType.DEVICE_NAME, "emulator-5554");
        dc.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
        dc.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, "60");
        //dc.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ESPRESSO);
        dc.setCapability("appPackage", "com.abmcloud");
        dc.setCapability("appActivity", ".MainActivity");
        dc.setCapability("noReset", true);
        driver = new AndroidDriver<MobileElement>(new URL("http://0.0.0.0:4723/wd/hub"), dc);
    }

    @Test
    public static void basic() throws InterruptedException, IOException {

        writeLog("Step1. Login");
        ChoseLogin("Admin");
        Thread.sleep(1000);

        while (true) {
            List<String> taskList = Tasks.getTaskList();
            for (String s : taskList) {
                if ((s.toLowerCase().contains("перемещение") || s.toLowerCase().contains("пополнение")) && !s.toLowerCase().contains("транзит")) {
                    Tasks.choseTask(s);
                    Tasks.performMovingTask();
                }
                if (s.toLowerCase().contains("контейнер")) {
                    Tasks.choseTask(s);
                    Thread.sleep(1000);
                    Tasks.performContainerTask();
                }
                if (s.toLowerCase().contains("прием") && !s.toLowerCase().contains("размещение") && !s.toLowerCase().contains("транзит")) {
                    Tasks.choseTask(s);
                    Tasks.performReceivingTask();
                }
                if (s.toLowerCase().contains("размещение")) {
                    Tasks.choseTask(s);
                    Tasks.performMovingTask();
                }
                if (s.toLowerCase().contains("отбор")) {
                    Tasks.choseTask(s);
                    Tasks.performPickingTask();
                }
            }
        }
    }


    @AfterTest
    private static void closeAll() throws IOException {
        log.close();
        driver.close();
    }

    private static String getLogDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(Calendar.getInstance().getTime());
    }

    public static void writeLog (String text) {
        try {
            log.write(getLogDate() + " : " + text + "\n");
            log.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void ChoseLogin (String login) {
        MobileElement meUsers = driver.findElementById("com.abmcloud:id/spinnerUserSel");
        //fill user-password
        HashMap<String, String> users = new HashMap<String, String>();
        users.put("Admin","123");
        users.put("AdminVal","123");
        users.put("AdminSN","123");

        writeLog("Enter login: "+login);
        meUsers.sendKeys(login);

        MobileElement mePassword = driver.findElementById("com.abmcloud:id/passwordUser");
        // get password by username
        writeLog("Get password by username: "+login);
        String password = users.get(login);

        writeLog("Send password");
        mePassword.sendKeys(password);
        driver.pressKey(new KeyEvent(AndroidKey.ENTER));

        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.abmcloud:id/swipe_container")));

    }


}
