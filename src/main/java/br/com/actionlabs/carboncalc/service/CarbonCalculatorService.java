package br.com.actionlabs.carboncalc.service;

import br.com.actionlabs.carboncalc.dto.*;
import br.com.actionlabs.carboncalc.model.*;
import br.com.actionlabs.carboncalc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarbonCalculatorService {

    private final CarbonCalculationRepository calculationRepository;
    private final EnergyEmissionFactorRepository energyRepository;
    private final SolidWasteEmissionFactorRepository wasteRepository;
    private final TransportationEmissionFactorRepository transportRepository;

    public StartCalcResponseDTO startCalculation(StartCalcRequestDTO request) {
        CarbonCalculation calc = new CarbonCalculation();
        calc.setName(request.getName());
        calc.setEmail(request.getEmail());
        calc.setUf(request.getUf());
        calc.setPhoneNumber(request.getPhoneNumber());

        calc = calculationRepository.save(calc);

        StartCalcResponseDTO response = new StartCalcResponseDTO();
        response.setId(calc.getId());
        return response;
    }

    public UpdateCalcInfoResponseDTO updateInfo(UpdateCalcInfoRequestDTO request) {
        CarbonCalculation calc = calculationRepository.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cálculo não encontrado"));

        calc.setEnergyConsumption(request.getEnergyConsumption());
        calc.setSolidWasteTotal(request.getSolidWasteTotal());
        calc.setRecyclePercentage(request.getRecyclePercentage());

        if (request.getTransportation() != null) {
            calc.setTransportation(request.getTransportation().stream().map(dto -> {
                CarbonCalculation.TransportationInfo info = new CarbonCalculation.TransportationInfo();
                info.setType(dto.getType());
                info.setMonthlyDistance(dto.getMonthlyDistance());
                return info;
            }).collect(Collectors.toList()));
        }

        calculationRepository.save(calc);

        UpdateCalcInfoResponseDTO response = new UpdateCalcInfoResponseDTO();
        response.setSuccess(true);
        return response;
    }

    public CarbonCalculationResultDTO getResult(String id) {
        CarbonCalculation calc = calculationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cálculo não encontrado"));

        double energyEmission = calculateEnergy(calc);
        double wasteEmission = calculateWaste(calc);
        double transportEmission = calculateTransport(calc);

        CarbonCalculationResultDTO result = new CarbonCalculationResultDTO();
        result.setEnergy(energyEmission);
        result.setSolidWaste(wasteEmission);
        result.setTransportation(transportEmission);
        result.setTotal(energyEmission + wasteEmission + transportEmission);

        return result;
    }

    private double calculateEnergy(CarbonCalculation calc) {
        EnergyEmissionFactor factor = energyRepository.findById(calc.getUf())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fator de energia não encontrado para a UF: " + calc.getUf()));
        
        return calc.getEnergyConsumption() * factor.getFactor();
    }

    private double calculateWaste(CarbonCalculation calc) {
        SolidWasteEmissionFactor factor = wasteRepository.findById(calc.getUf())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fator de lixo não encontrado para a UF: " + calc.getUf()));

        double recyclePct = calc.getRecyclePercentage();
        double nonRecyclePct = 1.0 - recyclePct;

        double recEmission = (calc.getSolidWasteTotal() * recyclePct) * factor.getRecyclableFactor();
        double nonRecEmission = (calc.getSolidWasteTotal() * nonRecyclePct) * factor.getNonRecyclableFactor();

        return recEmission + nonRecEmission;
    }

    private double calculateTransport(CarbonCalculation calc) {
        double totalTransportEmission = 0.0;

        for (CarbonCalculation.TransportationInfo info : calc.getTransportation()) {
            TransportationEmissionFactor factor = transportRepository.findById(info.getType())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fator não encontrado para transporte: " + info.getType()));
            
            totalTransportEmission += info.getMonthlyDistance() * factor.getFactor();
        }

        return totalTransportEmission;
    }
}