package com.airbus.optim.utils;

import com.airbus.optim.dto.ExistsOpenedExerciseDTO;
import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.Lever;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.User;
import com.airbus.optim.entity.WorkloadEvolution;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.SiglumRepository;
import com.airbus.optim.repository.UserRepository;
import com.airbus.optim.repository.WorkloadEvolutionRepository;
import java.time.LocalDate;
import java.time.ZoneId;

import com.airbus.optim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class Utils {

    @Autowired
    private SiglumRepository siglumRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkloadEvolutionRepository workloadEvolutionRepository;

    @Autowired
    private UserService userService;

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Arrays.stream(str.toLowerCase().split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    public static List<Sort.Order> getSortOrders(String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();
        for (String sortOrder : sort) {
            String[] _sort = sortOrder.split(",");
            orders.add(new Sort.Order(Sort.Direction.ASC, _sort[0]));
        }
        return orders;
    }

    public boolean checkOverlapOfLevers(Lever newLever, Long employeeId) {
        Optional<Employee> employeeOptional = employeeRepository.findById(employeeId);
        if(!employeeOptional.isPresent()){
            return false;
        }

        for (Lever existingLever : employeeOptional.get().getLevers()) {
            if (doLeversOverlap(existingLever, newLever)) {
                return true;
            }
        }
        return false;
    }
    private boolean doLeversOverlap(Lever lever1, Lever lever2) {
        Instant start1 = lever1.getStartDate();
        Instant end1 = lever1.getEndDate();
        Instant start2 = lever2.getStartDate();
        Instant end2 = lever2.getEndDate();

        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }

        return (start1.isBefore(end2) || start1.equals(end2)) && (end1.isAfter(start2) || end1.equals(start2));
    }

    public <T> void setNextAvailableId(T entity, IdentifiableRepository<T> repository) {
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            if (idField.get(entity) == null) {
                Long nextId = repository.findNextAvailableId();
                if (nextId == null) {
                    nextId = 1L;
                }
                idField.set(entity, nextId);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set next available ID", e);
        }
    }

    public String getExerciseOP(int yearFilter) {
        return Constants.EXERCISE_OPERATION_PLANNING+Integer.toString(Math.floorMod(yearFilter, 100));
    }

    public String getExerciseFCII(int yearFilter) {
        return Constants.EXERCISE_FORECAST+Integer.toString(Math.floorMod(yearFilter, 100));
    }

    public String getExercise(String exercise, int yearFilter) {
        if(Constants.EXERCISE_OPERATION_PLANNING.equals(exercise)) {
            return getExerciseOP(yearFilter);
        } else if (Constants.EXERCISE_FORECAST.equals(exercise)) {
            return getExerciseFCII(yearFilter);
        } else {
            return "";
        }
    }

    public String getExerciseName() {
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());
        String exercise;

        if (currentDate.isAfter(LocalDate.parse(String.valueOf(currentDate.getYear()) + "-03-01")) &&
                currentDate.isBefore(LocalDate.parse(String.valueOf(currentDate.getYear()) + "-09-01"))) {
            exercise = Constants.EXERCISE_OPERATION_PLANNING;
        } else {
            exercise = Constants.EXERCISE_FORECAST;
        }

        return exercise;
    }

    public boolean filterYearComprobation(int yearFilter) {
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());
        return ((yearFilter < currentDate.getYear()-1) || (yearFilter > currentDate.getYear()+5));
    }

    public List<Siglum> getSiglumVisibilityList(String userSelected) {
        String siglumVisible = getUserInSession(userSelected).getSiglumVisible();

        if (siglumVisible == null || siglumVisible.isEmpty()) {
            return Collections.emptyList();
        }

        String[] siglumsArray = siglumVisible.split(",");
        List<Siglum> userSiglumList = new ArrayList<>();

        for (String siglum : siglumsArray) {
            userSiglumList.addAll(getVisibleSiglums(siglum, userSelected));
        }

        return userSiglumList;
    }

    public User getUserInSession(String userString) {
        // TODO: get email by token
        return userRepository.findOneByEmailIgnoreCase(userString)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<Siglum> getVisibleSiglums(String siglum, String email) {
        if (email != null && !email.isEmpty() && isSuperUser(email)) {
            return siglumRepository.findAll();
        }

        List<Siglum> visibleSiglums = new ArrayList<>();
        User currentUser = getUserInSession(email);

        if (currentUser != null) {
            Optional.ofNullable(currentUser.getSiglum())
                    .ifPresent(userSiglum -> visibleSiglums.addAll(
                            siglumRepository.findBySiglumHRStartingWith(userSiglum.getSiglumHR())
                    ));

            Optional.ofNullable(currentUser.getSiglumVisible())
                    .filter(siglums -> !siglums.isEmpty())
                    .ifPresent(visibleSiglumsString -> {
                        List<String> visibleSiglumList = Arrays.stream(visibleSiglumsString.split("[,;]"))
                                .map(String::trim)
                                .toList();
                        visibleSiglums.addAll(siglumRepository.findBySiglumHRIn(visibleSiglumList));
                    });
        }

        if (siglum != null) {
            visibleSiglums.addAll(siglumRepository.findBySiglumHRStartingWith(siglum));
        }

        return visibleSiglums;
    }

    public boolean isSuperUser(String email) {
        List<String> superUserRoles = Arrays.asList(
            //    Constants.USER_ROLE_HO_T1Q,  DESHABILITADO HASTA QUE FUNCIONEN LAS VALIDACIONES CON DOS O MAS NIVELES DE JERARQUIA
                Constants.USER_ROLE_WL_DELEGATE,
                Constants.USER_ROLE_ADMIN,
                Constants.USER_ROLE_HR_SUPERBOSS,
                Constants.USER_ROLE_FINANCE_SUPERBOSS
        );

        if (email != null) {
            User currentUser = getUserInSession(email);

            if (currentUser != null && currentUser.getRoles() != null) {
                String roles = currentUser.getRoles();
                for (String superUserRole : superUserRoles) {
                    if (roles.toLowerCase().contains(superUserRole.toLowerCase())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public List<Siglum> getEditableSiglums(String userSelected) {
        List<Siglum> siglumVisible = null;

        if (isSuperUser(userSelected)) {
            return getVisibleSiglums(null, userSelected);
        } else {
            Siglum mySiglum = getUserInSession(userSelected).getSiglum();
            siglumVisible = List.of(mySiglum);
        }

        List<String> targetStatus = List.of(
                Constants.WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED,
                Constants.WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED,
                Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5,
                Constants.WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6,
                Constants.WORKLOAD_EVOLUTION_STATUS_OPENED
        );

        String lastExercise = "";
        try {
            lastExercise = getLastOpenedExerciseOrThrow().getExercise();
        } catch (Exception e) {
            return Collections.emptyList();
        }

        return workloadEvolutionRepository.findSiglumBySiglumAndStatusAndExerciseIn(siglumVisible, targetStatus, lastExercise);
    }

    public String getNumberSiglums(String userSelected) {
        User currentUser = getUserInSession(userSelected);
        Siglum siglum = currentUser.getSiglum();

        if (siglum == null) {
            throw new IllegalArgumentException("Siglum not found for the current user");
        }

        if (!Objects.equals(siglum.getSiglumHR(), siglum.getSiglum6())) {
            return Constants.USER_SIGLUM_HR;
        }
        if (!Objects.equals(siglum.getSiglum6(), siglum.getSiglum5())) {
            return Constants.USER_SIGLUM_6;
        }
        if (!Objects.equals(siglum.getSiglum5(), siglum.getSiglum4())) {
            return Constants.USER_SIGLUM_5;
        }
        if (!Objects.equals(siglum.getSiglum4(), siglum.getSiglum3())) {
            return Constants.USER_SIGLUM_4;
        }
        return Constants.USER_SIGLUM_3;
    }

    public ExistsOpenedExerciseDTO existsOpenedExercise() {
        try {
            getLastOpenedExerciseOrThrow();
            return new ExistsOpenedExerciseDTO(true);
        } catch(Exception e){
            return new ExistsOpenedExerciseDTO(false);
        }
    }

    public WorkloadEvolution getLastOpenedExerciseOrThrow() {
        List<WorkloadEvolution> openedExercises = workloadEvolutionRepository.findByStatusAndExerciseNotInClosed(
                Constants.WORKLOAD_EVOLUTION_STATUS_OPENED,
                Constants.WORKLOAD_EVOLUTION_STATUS_CLOSED
        );

        if (openedExercises == null || openedExercises.isEmpty()) {
            throw new IllegalArgumentException("No opened exercise found.");
        }

        return workloadEvolutionRepository.findTopByStatusOrderBySubmitDateDesc(Constants.WORKLOAD_EVOLUTION_STATUS_OPENED)
                .orElseThrow(() -> new IllegalArgumentException("No opened exercise found."));
    }
}
