package com.airbus.optim.utils;

import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.Lever;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.User;
import com.airbus.optim.repository.EmployeeRepository;
import com.airbus.optim.repository.UserRepository;
import com.airbus.optim.service.SiglumService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class Utils {

    @Autowired
    private SiglumService siglumService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

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

    public String getExerciseName() {
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());
        String exercise;

        if (currentDate.isAfter(LocalDate.parse(String.valueOf(currentDate.getYear()) + "-03-01")) &&
                currentDate.isBefore(LocalDate.parse(String.valueOf(currentDate.getYear()) + "-09-01"))) {
            exercise = getExerciseOP(currentDate.getYear());
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
        String[] siglumsArray = getUserInSession(userSelected).getSiglumVisible().split(",");
        List<Siglum> userSiglumList = new ArrayList<>();

        for(String siglum : siglumsArray) {
            userSiglumList.addAll(siglumService.getVisiblesSiglums(siglum));
        }

        return userSiglumList;
    }

    public User getUserInSession(String userSelected) {
        //String email = "SecurityContextHolder.getContext().getAuthentication().getEmail()";
        Optional<User> user = userRepository.findOneByLogin(userSelected);
        return (user.isPresent()) ? user.get() : new User();
    }
}
