package com.trampo.process;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.trampo.process.domain.CcmPlus;

@Component
@ConfigurationProperties("trampo")
public class TrampoConfig {

  private List<CcmPlus> ccmpluses;

  public List<CcmPlus> getCcmpluses() {
    return ccmpluses;
  }

  public void setCcmpluses(List<CcmPlus> ccmpluses) {
    this.ccmpluses = ccmpluses;
  }
}