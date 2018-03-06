package com.trampo.process;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.trampo.process.domain.CcmPlus;

@Component
@ConfigurationProperties("trampo")
public class TrampoConfig {

  private List<CcmPlus> ccmplus;

  public List<CcmPlus> getCcmplus() {
    return ccmplus;
  }

  public void setCcmplus(List<CcmPlus> ccmplus) {
    this.ccmplus = ccmplus;
  }
}
