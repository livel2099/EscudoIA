package com.livel.escudo.risk;

import jakarta.persistence.*;

@Entity
@Table(name = "risk_config")
public class RiskConfigEntity {
    @Id @Column(name = "component_code") private String componentCode;
    @Column(nullable = false) private int weight;
    @Column(nullable = false) private String version;

    protected RiskConfigEntity() {}
    public String getComponentCode() { return componentCode; }
    public int getWeight() { return weight; }
    public String getVersion() { return version; }
    public void update(int weight, String version) { this.weight = weight; this.version = version; }
}

