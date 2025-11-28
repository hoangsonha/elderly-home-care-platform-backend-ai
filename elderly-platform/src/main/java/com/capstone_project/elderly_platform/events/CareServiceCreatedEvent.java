package com.capstone_project.elderly_platform.events;

import com.capstone_project.elderly_platform.pojos.CareService;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CareServiceCreatedEvent extends ApplicationEvent {
    
    private final CareService careService;
    
    public CareServiceCreatedEvent(Object source, CareService careService) {
        super(source);
        this.careService = careService;
    }
}





