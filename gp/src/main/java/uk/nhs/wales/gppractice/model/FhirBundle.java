package uk.nhs.wales.gppractice.model;

import java.util.List;

public class FhirBundle {
    private String resourceType;
    private String type;
    private List<FhirEntry> entry;

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<FhirEntry> getEntry() { return entry; }
    public void setEntry(List<FhirEntry> entry) { this.entry = entry; }

    public static class FhirEntry {
        private FhirResource resource;

        public FhirResource getResource() { return resource; }
        public void setResource(FhirResource resource) { this.resource = resource; }
    }
}

