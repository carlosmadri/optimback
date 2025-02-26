package com.airbus.optim.service;

import com.airbus.optim.entity.PPSID;
import com.airbus.optim.repository.PPSIDRepository;
import com.airbus.optim.repository.WorkloadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PPSIDService {

    @Autowired
    private PPSIDRepository ppsidRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    public PPSID savePPSID(PPSID ppsid) {
        return ppsidRepository.save(ppsid);
    }

    public ResponseEntity<PPSID> updatePPSID(Long id, PPSID ppsidDetails) {
        Optional<PPSID> optionalPPSID = ppsidRepository.findById(id);

        if (optionalPPSID.isPresent()) {
            PPSID existingPPSID = optionalPPSID.get();
            updatePPSIDFields(existingPPSID, ppsidDetails);
            PPSID updatedPPSID = ppsidRepository.save(existingPPSID);
            return ResponseEntity.ok(updatedPPSID);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private void updatePPSIDFields(PPSID existingPPSID, PPSID ppsidDetails) {
        if (ppsidDetails.getPpsid() != null) {
            existingPPSID.setPpsid(ppsidDetails.getPpsid());
        }
        if (ppsidDetails.getPpsidName() != null) {
            existingPPSID.setPpsidName(ppsidDetails.getPpsidName());
        }
        if (ppsidDetails.getMuCode() != null) {
            existingPPSID.setMuCode(ppsidDetails.getMuCode());
        }
        if (ppsidDetails.getMuText() != null) {
            existingPPSID.setMuText(ppsidDetails.getMuText());
        }
        if (ppsidDetails.getBusinessLine() != null) {
            existingPPSID.setBusinessLine(ppsidDetails.getBusinessLine());
        }
        if (ppsidDetails.getProgramLine() != null) {
            existingPPSID.setProgramLine(ppsidDetails.getProgramLine());
        }
        if (ppsidDetails.getProductionCenter() != null) {
            existingPPSID.setProductionCenter(ppsidDetails.getProductionCenter());
        }
        if (ppsidDetails.getBusinessActivity() != null) {
            existingPPSID.setBusinessActivity(ppsidDetails.getBusinessActivity());
        }
        if (ppsidDetails.getBacklogOrderIntake() != null) {
            existingPPSID.setBacklogOrderIntake(ppsidDetails.getBacklogOrderIntake());
        }
    }
}

