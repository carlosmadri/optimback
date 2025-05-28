package com.airbus.optim.dto;

import com.airbus.optim.entity.Siglum;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPropertiesDTO {
    String userRol;
    Siglum siglum;
    List<Siglum> siglumsVisible;
}
