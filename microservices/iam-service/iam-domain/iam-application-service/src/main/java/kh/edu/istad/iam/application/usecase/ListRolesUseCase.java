package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.query.RoleFilterQuery;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.RoleSummary;

public interface ListRolesUseCase {
    PageResult<RoleSummary> execute(RoleFilterQuery query);
}
