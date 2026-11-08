package kh.edu.istad.iam.application.usecase;

import kh.edu.istad.iam.application.query.UserFilterQuery;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.UserSummary;

public interface ListUsersUseCase {
    PageResult<UserSummary> execute(UserFilterQuery query);
}
