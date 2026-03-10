package br.com.actionlabs.carboncalc.model;

import br.com.actionlabs.carboncalc.enums.TransportationType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "carbon_calculations")
public class CarbonCalculation {

    @Id
    private String id;
    private String name;
    private String email;
    private String uf;
    private String phoneNumber;

    private int energyConsumption;
    private int solidWasteTotal;
    private double recyclePercentage;
    
    private List<TransportationInfo> transportation = new ArrayList<>();

    @Data
    public static class TransportationInfo {
        private TransportationType type;
        private int monthlyDistance;
    }
}