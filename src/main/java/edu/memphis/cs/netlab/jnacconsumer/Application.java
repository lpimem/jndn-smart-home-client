package edu.memphis.cs.netlab.jnacconsumer;

import edu.memphis.cs.netlab.nacapp.ConsumerDBSource;
import edu.memphis.cs.netlab.nacapp.ConsumerSQLiteDBSource;
import edu.memphis.cs.netlab.nacapp.ConsumerWrapper;
import edu.memphis.cs.netlab.nacapp.Global;

import net.named_data.jndn.Name;
import net.named_data.jndn.encrypt.ConsumerDb;
import net.named_data.jndn.encrypt.Sqlite3ConsumerDb;

import java.io.File;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Description:
 * <p>
 * Author: lei
 * Date  : 7/7/17.
 */
public class Application {
  private static Logger logger = Global.LOGGER;


  public static void main(String[] args) {
    final Name group = new Name(Global.LOCAL_HOME + "/READ");
    final TemperatureReader reader = new TemperatureReader(group);

    reader.init(
        new Name(Global.DEVICE_PREFIX + "/home-client"),
        new ConsumerSQLiteDBSource(":memory:"));

    final TemperatureReader.OnDataCallback onData = new TemperatureReader.OnDataCallback() {
      @Override
      public void onData(String desc, int temperature) {
        logger.log(Level.INFO, String.format(Locale.ENGLISH, "%s: %d", desc, temperature));
      }

      @Override
      public void onFail(String reason) {
        logger.log(Level.INFO, String.format(Locale.ENGLISH, "ERROR: %s", reason));
      }
    };

    final Runnable onRequestPermissionSuccess = new Runnable() {
      @Override
      public void run() {
        logger.info("permission granted.");
        reader.read(new Name("bedroom"), onData);
      }
    };

    final Runnable onRequestPermissionFailed = new Runnable() {
      @Override
      public void run() {
        logger.warning("permission not granted.");
      }
    };

    final Runnable onRegisterIdentity = new Runnable() {
      @Override
      public void run() {
        reader.requestGrantPermission("bedroom", onRequestPermissionSuccess, onRequestPermissionFailed);
      }
    };

    logger.info("registerIdentity...");
    reader.registerIdentity(onRegisterIdentity);
    reader.startFaceProcessing();
  }
}