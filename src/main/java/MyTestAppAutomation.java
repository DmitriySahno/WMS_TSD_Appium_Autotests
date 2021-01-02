import com.google.common.collect.ImmutableMap;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;


public class MyTestAppAutomation {

    public static AndroidDriver<MobileElement> driver;

    @BeforeTest
    public static void setup() throws MalformedURLException {
        DesiredCapabilities dc = new DesiredCapabilities();
        dc.setCapability(MobileCapabilityType.DEVICE_NAME, "emulator-5554");
        dc.setCapability(MobileCapabilityType.PLATFORM_NAME, "android");
        dc.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, "60");
        dc.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ESPRESSO);
//1        dc.setCapability("appPackage", "com.application.test");
        //dc.setCapability("appPackage", "com.example.myapplication");
        dc.setCapability("appPackage", "com.example.myapplication");
        dc.setCapability("appActivity", ".MainActivity");
//2        dc.setCapability(MobileCapabilityType.APP, "app-debug.apk");
//        dc.setCapability("appActivity", ".MyApp");
        dc.setCapability("forceEspressoRebuild", true);
        dc.setCapability("noReset", true);
        dc.setCapability("showGradleLog", true);
        driver = new AndroidDriver<MobileElement>(new URL("http://0.0.0.0:4723/wd/hub"), dc);
    }


    @Test
    public static void test () {
        ImmutableMap<String, Object> scriptArgs = ImmutableMap.of(
                "target", "application",
                "methods", Arrays.asList(ImmutableMap.of(
                        "name", "getText")));
        driver.execute("mobile:backdoor", scriptArgs);
    }

}
