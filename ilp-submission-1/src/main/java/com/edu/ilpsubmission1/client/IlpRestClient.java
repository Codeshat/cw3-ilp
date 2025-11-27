package com.edu.ilpsubmission1.client;

import com.edu.ilpsubmission1.dtos.Drone;
import com.edu.ilpsubmission1.dtos.DroneForServicePoint;
import com.edu.ilpsubmission1.dtos.ServicePoint;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;


@Component
public class IlpRestClient {

    private final WebClient client;
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public IlpRestClient(WebClient ilpWebClient, RestTemplate restTemplate, String ilpBaseUrl) {
        this.client = ilpWebClient;
        this.restTemplate = restTemplate;
        this.baseUrl = ilpBaseUrl;
    }

    public List<Map<String, Object>> getServicePoints() {
        return client.get().uri("/service-points")
                .retrieve().bodyToMono(List.class).block();
    }

    public List<Map<String, Object>> getRestrictedAreas() {
        return client.get().uri("/restricted-areas")
                .retrieve().bodyToMono(List.class).block();
    }

    public List<Map<String,Object>> getDrones() {
        return client.get().uri("/drones")
                .retrieve().bodyToMono(List.class).block();
    }

    public List<Map<String,Object>> getDronesForServicePoints() {
        return client.get().uri("/drones-for-service-points")
                .retrieve().bodyToMono(List.class).block();
    }

    public Map<String,Object> getDroneByIdRaw(int id) {
        return client.get().uri("/drones/{id}", id)
                .retrieve().bodyToMono(Map.class).block();
    }
    public Drone[] getDronesJson() {
        String url = baseUrl + "/drones";
        return restTemplate.getForObject(url, Drone[].class);
    }

    public DroneForServicePoint[] getDronesForServicePointsJson() {
        String url = baseUrl + "/drones-for-service-points";
        return restTemplate.getForObject(url, DroneForServicePoint[].class);
    }
    public ServicePoint[] getServicePointsJson() {
        String url = baseUrl + "/service-points";
        return restTemplate.getForObject(url, ServicePoint[].class);
    }



}
