package edu.memphis.cs.netlab.jnacconsumer;

import edu.memphis.cs.netlab.nacapp.*;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.encrypt.Consumer;
import net.named_data.jndn.encrypt.EncryptError;
import net.named_data.jndn.security.certificate.Certificate;
import net.named_data.jndn.util.Blob;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.memphis.cs.netlab.nacapp.Global.*;

/**
 * Description:
 * <p>
 * Author: lei
 */

public class TemperatureReader extends NACNode {
  private final static String TAG = NACNode.class.getName();

  private static final Logger LOGGER = Global.LOGGER;

  public TemperatureReader(Name namespace, Name group, ConsumerDBSource dbSource) {
    super(namespace);
    m_group = group;
    initConsumer(namespace, dbSource);
  }

  public interface OnDataCallback {
    void onData(String desc, int temperature);

    void onFail(String reason);
  }

  public void read(final Name location, final OnDataCallback callback) {
    final Name servicePrefix = new Name(
        LOCAL_HOME + SAMPLE + LOCATION + location + "/temperature/" + Utils.nowIsoString());
    m_consumer.consume(servicePrefix, new Consumer.OnConsumeComplete() {
      @Override
      public void onConsumeComplete(Data data, Blob result) {
        String temp = new String(result.getImmutableArray());
        LOGGER.info("GOT temperature: " + String.valueOf(temp));
        callback.onData("success", (int) (Double.parseDouble(temp)));
      }
    }, new EncryptError.OnError() {
      @Override
      public void onError(EncryptError.ErrorCode errorCode, String message) {
        LOGGER.log(Level.SEVERE, "Error consuming " + servicePrefix.toUri() + ": " + errorCode.name());
        LOGGER.log(Level.SEVERE, message);
        try {
          callback.onFail(errorCode.name());
        } catch (Throwable e) {
          LOGGER.log(Level.SEVERE, e.getMessage());
        }
      }
    });
  }

  public void requestGrantPermission(String location, final Runnable onSuccess, final Runnable onFail) {
    final String datatype = LOCATION + Utils.nameComponent(location);
    super.requestGrantPermission(m_consumerWrapper.getCertificate().getName(), datatype, onSuccess, onFail);
  }

  public void registerIdentity(final Runnable onSuccess) {
    OnRegisterIdentitySuccess callback = new OnRegisterIdentitySuccess() {
      @Override
      public void onNewCertificate(Certificate cert) {
        m_consumerWrapper.setCertificate(cert);
        onSuccess.run();
      }
    };

    Data cert = m_consumerWrapper.getCertificate();
    Name certName = cert.getName();
    super.registerIdentity(certName, cert, callback);
  }

  /**
   * Create consumer instance with name "${appPrefix}/Consumer"
   *
   * @param appPrefix
   */
  private void initConsumer(Name appPrefix, ConsumerDBSource dbSource) {
    Name consumerName = new Name(appPrefix);
    consumerName.append("Consumer");
    m_consumerWrapper = ConsumerWrapper.make(consumerName, m_group, m_keychain, m_face, dbSource);
    m_consumer = m_consumerWrapper.getConsumer();
  }

  public Consumer getConsumer() {
    return m_consumer;
  }

  public ConsumerWrapper getConsumerWrapper() {
    return m_consumerWrapper;
  }

  public Name getGroup() {
    return m_group;
  }

  private Consumer m_consumer;
  private ConsumerWrapper m_consumerWrapper;
  private final Name m_group;
}
