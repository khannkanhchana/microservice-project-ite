package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.query.PermissionFilterQuery;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.PermissionResult;

public interface ListPermissionsUseCase {
    PageResult<PermissionResult> execute(PermissionFilterQuery query);
}
