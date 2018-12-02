package com.sjsu.cmpe.sstreet.mirroringserver.service;


import com.sjsu.cmpe.sstreet.mirroringserver.model.Location;
import com.sjsu.cmpe.sstreet.mirroringserver.model.SmartCluster;
import com.sjsu.cmpe.sstreet.mirroringserver.model.SmartNode;
import com.sjsu.cmpe.sstreet.mirroringserver.repository.mysql.SmartNodeRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SmartNodeService {

    private final String NODES_UNREGISTERED_API = "/smart_node/nodes/unregistered";
    private final String NODE_REGISTERED_EVENT_API = "/smart_node/node/registered";

    @Value(value = "${cluster.device.url}")
    private String clusterURL;


    private SmartNodeRepository smartNodeRepository;
    private ModelMapper modelMapper;
    private RestTemplate restTemplate;
    private Logger log;

    @Autowired
    public SmartNodeService(SmartNodeRepository smartNodeRepository, RestTemplate restTemplate, Logger log) {

        this.smartNodeRepository = smartNodeRepository;
        this.restTemplate = restTemplate;
        this.log = log;
    }

    public ResponseEntity<String> createSmartNode(SmartNode smartNode) {


        SmartNode savedSmartNode = smartNodeRepository.save(smartNode);

        if(null != savedSmartNode){

            return ResponseEntity.ok("Smart Node Created with ID: "+savedSmartNode.getIdSmartNode());
        }else{

            return new ResponseEntity<>("A Smart Node at requested location already exists", HttpStatus.BAD_REQUEST);
        }

    }

    public ResponseEntity<String> updateSmartNode(SmartNode smartNode){



        Optional<SmartNode> smartNodeResult = smartNodeRepository.findById(smartNode.getIdSmartNode());

        smartNodeResult.ifPresent(result->{
            smartNode.setName(result.getName());
            smartNode.setMake(result.getMake());
            smartNode.setModel(result.getModel());
            smartNode.setInstallationDate(result.getInstallationDate());
            smartNode.setSmartCluster(result.getSmartCluster());

        });

        if(smartNodeResult.isPresent()){

            if(null != smartNodeRepository.save(smartNode)){
                return ResponseEntity.ok("Smart Node updated");

            }else{
                return new ResponseEntity<>("Smart Node  Failed",HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }else{
            return new ResponseEntity<>("Smart Node with ID: " + smartNode.getIdSmartNode()+" does not exist",HttpStatus.BAD_REQUEST);
        }

    }

    public List<SmartNode> getAllSmartNodes(){

        Iterable<SmartNode> smartNodeIterable = smartNodeRepository.findAll();
        List<SmartNode> smartNodeList  = new ArrayList<>();

        smartNodeIterable.forEach(smartNode ->
            smartNodeList.add(modelMapper.map(smartNode, SmartNode.class))

        );

        return smartNodeList;
    }

    public SmartNode getSmartNodeById(Integer id){

        Optional<SmartNode> smartNodeOptional = smartNodeRepository.findById(id);
        List<SmartNode> smartNode = new ArrayList<>();

        if(!smartNodeOptional.isPresent()) {

            return null;
        }


        return smartNode.get(0);

    }

    public SmartNode getSmartNodeByName(String name){

        Optional<SmartNode> smartNodeOptional = smartNodeRepository.findByName(name);
        List<SmartNode> smartNodeList = new ArrayList<>();

        if(!smartNodeOptional.isPresent()) {

            return null;
        }

        smartNodeOptional.ifPresent(smartNode ->
                smartNodeList.add(modelMapper.map(smartNode,SmartNode.class))

        );

        return smartNodeList.get(0);

    }

    public SmartNode getSmartNodeByLocation(Location location){


        SmartNode smartNode = smartNodeRepository.findByLocation(location);

        return modelMapper.map(smartNode, SmartNode.class);


    }

    public List<SmartNode> getSmartNodeBySmartCluster(SmartCluster smartCluster) {

        return smartNodeRepository.findBySmartCluster(smartCluster);

    }

    public ResponseEntity<String> deleteSmartNodeById(Integer id){

        smartNodeRepository.deleteById(id);
        return ResponseEntity.ok("Smart Node Successfully Deleted");

    }

    public ResponseEntity<String> deleteSmartNodeByName(String name){

        smartNodeRepository.deleteByName(name);
        return ResponseEntity.ok("Smart Node Successfully Deleted");

    }

    public ResponseEntity<String> deleteSmartNodeBySmartCluster(SmartCluster smartCluster){

        smartNodeRepository.deleteBySmartCluster(smartCluster);
        return ResponseEntity.ok("Smart Node Successfully Deleted");

    }

    public List<SmartNode> getUnregisteredNodes(){

        String url = clusterURL + NODES_UNREGISTERED_API;
        ResponseEntity<List<SmartNode>> response = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<SmartNode>>(){});
        List<SmartNode> nodes = response.getBody();

        return  nodes;
    }

    public void registeredNodeEvent(SmartNode smartNode){
        log.info("Call the cluster device with registered node");
        String url = clusterURL + NODE_REGISTERED_EVENT_API;
        HttpEntity<SmartNode> httpEntity = new HttpEntity<>(smartNode);
        restTemplate.put(url, httpEntity);
    }
}
