package uk.nhs.wales.gppractice.model;

public class FhirResource {
    private String resourceType;
    private String id;

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

