package kh.edu.istad.iam.application.query;

public record PermissionFilterQuery(String search, String resource, String action,
                                    Boolean system, Boolean active, Boolean deleted,
                                    int page, int size, String sortBy, SortDirection sortDirection) {
    public PermissionFilterQuery {
        if (page < 0) throw new IllegalArgumentException("page cannot be negative");
        if (size < 1 || size > 200) throw new IllegalArgumentException("size must be between 1 and 200");
        if (sortDirection == null) sortDirection = SortDirection.ASC;
        if (sortBy == null || sortBy.isBlank()) sortBy = "name";
    }
}
