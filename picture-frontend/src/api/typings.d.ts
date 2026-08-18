declare namespace API {
  type AddUserRequest = {
    username?: string;
    useraccount?: string;
    gender?: number;
    phone?: string;
    userLevel?: string;
  };

  type AdminCheckAvatarRequest = {
    userId?: number | string;
    checkResult?: number;
    checkMessage?: string;
  };

  type AdminCheckPictureBatchRequest = {
    picIds?: Array<number | string>;
    checkResult?: number;
    checkMessage?: string;
  };

  type AdminCheckPictureRequest = {
    picId?: number | string;
    checkResult?: number;
    checkMessage?: string;
  };

  type AlterLevelRequest = {
    spaceId?: number | string;
    alterLevel?: number;
  };

  type BaseResponseBoolean = {
    code?: number;
    message?: string;
    data?: boolean;
  };

  type BaseResponseIPagePictureVO = {
    code?: number;
    message?: string;
    data?: IPagePictureVO;
  };

  type BaseResponseListUserVO = {
    code?: number;
    message?: string;
    data?: UserVO[];
  };

  type BaseResponseListAvatarReviewVO = {
    code?: number;
    message?: string;
    data?: AvatarReviewVO[];
  };

  type BaseResponsePictureListVO = {
    code?: number;
    message?: string;
    data?: PictureListVO;
  };

  type BaseResponsePicturePageVO = {
    code?: number;
    message?: string;
    data?: PicturePageVO;
  };

  type BaseResponsePictureTagCategory = {
    code?: number;
    message?: string;
    data?: PictureTagCategory;
  };

  type BaseResponsePictureUploadVO = {
    code?: number;
    message?: string;
    data?: PictureUploadVO;
  };

  type BaseResponsePictureVO = {
    code?: number;
    message?: string;
    data?: PictureVO;
  };

  type BaseResponseSpace = {
    code?: number;
    message?: string;
    data?: Space;
  };

  type BaseResponseSpacePageVO = {
    code?: number;
    message?: string;
    data?: SpacePageVO;
  };

  type BaseResponseSpaceVO = {
    code?: number;
    message?: string;
    data?: SpaceVO;
  };

  type BaseResponseString = {
    code?: number;
    message?: string;
    data?: string;
  };

  type BaseResponseUploadAvatarVO = {
    code?: number;
    message?: string;
    data?: UploadAvatarVO;
  };

  type BaseResponseUserPagesVO = {
    code?: number;
    message?: string;
    data?: UserPagesVO;
  };

  type BaseResponseUserVO = {
    code?: number;
    message?: string;
    data?: UserVO;
  };

  type CreateSpaceRequest = {
    spaceName?: string;
  };

  type deleteByIdParams = {
    spaceId: number | string;
  };

  type DeleteRequest = {
    id?: number | string;
    ids?: Array<number | string>;
  };

  type getPictureByIdParams = {
    id: number | string;
  };

  type getUserByIdParams = {
    queryUserRequest: QueryUserRequest;
  };

  type IPagePictureVO = {
    size?: number | string;
    current?: number;
    pages?: number;
    records?: PictureVO[];
    total?: number | string;
  };

  type PictureListVO = {
    pictureList?: PictureVO[];
    targetCount?: number;
    successCount?: number;
  };

  type PicturePageVO = {
    pictureList?: PictureVO[];
    total?: number | string;
  };

  type PictureQueryRequest = {
    current?: number;
    pageSize?: number;
    sortFiled?: string;
    sortOrder?: string;
    id?: number | string;
    name?: string;
    introduction?: string;
    category?: string;
    searchText?: string;
    tags?: string[];
    pictureCheck?: number;
    userId?: number | string;
    spaceId?: number | string;
  };

  type PictureTagCategory = {
    tags?: string[];
    categorys?: string[];
  };

  type PictureUpdateRequest = {
    id?: number | string;
    name?: string;
    introduction?: string;
    category?: string;
    tags?: string[];
    spaceId?: number | string;
  };

  type PictureUploadByBatchRequest = {
    searchText?: string;
    count?: number;
    name?: string;
    category?: string;
    tags?: string[];
  };

  type PictureUploadFailVO = {
    size?: number | string;
    fileName?: string;
    message?: string;
  };

  type PictureUploadVO = {
    totalCount?: number;
    successCount?: number;
    failCount?: number;
    successPictureList?: PictureVO[];
    failPictureList?: PictureUploadFailVO[];
  };

  type PictureVO = {
    id?: number | string;
    url?: string;
    thumbnailUrl?: string;
    name?: string;
    introduction?: string;
    category?: string;
    tags?: string[];
    picsize?: number | string;
    picwidth?: number;
    picheight?: number;
    picscale?: number;
    picformat?: string;
    userId?: number | string;
    createdUser?: UserVO;
    createtime?: string;
    updatetime?: string;
    pictureCheck?: number;
    checkAdminId?: number | string;
    checkMessage?: string;
    spaceId?: number | string;
  };

  type QueryPageRequest = {
    current?: number;
    size?: number | string;
    sortField?: string;
    sortOrder?: string;
    id?: number | string;
    queryUsername?: string;
    queryUserAccount?: string;
    userLevel?: string;
    accountStatus?: number;
    profile?: string;
    gender?: number;
  };

  type querySpaceByIdParams = {
    spaceId: number | string;
  };

  type querySpacePageParams = {
    spaceQueryRequest: SpaceQueryRequest;
  };

  type QueryUserRequest = {
    id?: number | string;
  };

  type RegisterRequest = {
    username?: string;
    useraccount?: string;
    gender?: number;
    userpassword?: string;
    reUserPassword?: string;
    phone?: string;
  };

  type reloadPictureParams = {
    url?: string;
    id?: number | string;
    name?: string;
    category?: string;
    tags?: string[];
    introduction?: string;
  };

  type Space = {
    id?: number | string;
    spaceName?: string;
    spaceLevel?: number;
    maxSize?: number | string;
    usedSize?: number | string;
    maxCount?: number | string;
    usedCount?: number | string;
    userId?: number | string;
    createTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type SpacePageVO = {
    spaceVOList?: SpaceVO[];
    total?: number | string;
  };

  type SpaceQueryRequest = {
    current?: number;
    pageSize?: number;
    sortFiled?: string;
    sortOrder?: string;
    id?: number | string;
    spaceName?: string;
    spaceLevel?: number;
  };

  type SpaceUpdateRequest = {
    spaceId?: number | string;
    updatedName?: string;
  };

  type SpaceVO = {
    id?: number | string;
    spaceName?: string;
    spaceLevel?: number;
    maxSize?: number | string;
    usedSize?: number | string;
    maxCount?: number | string;
    usedCount?: number | string;
    userId?: number | string;
    createdUser?: UserVO;
    createTime?: string;
    updateTime?: string;
  };

  type testDownloadFileParams = {
    filename: string;
  };

  type UpdateSelfRequest = {
    username?: string;
    gender?: number;
    phone?: string;
    email?: string;
    profile?: string;
  };

  type UploadAvatarVO = {
    message?: string;
    status?: number;
    newURL?: string;
  };

  type AvatarReviewVO = {
    id?: number | string;
    userId?: number | string;
    username?: string;
    useraccount?: string;
    avatarUrl?: string;
    status?: number;
    submittedAt?: string;
    reviewedAt?: string;
    checkMessage?: string;
  };

  type UpdateUserRequest = {
    id?: number | string;
    username?: string;
    avatarurl?: string;
    gender?: number;
    phone?: string;
    email?: string;
    profile?: string;
    userLevel?: string;
    accountStatus?: number;
  };

  type uploadPicParams = {
    url?: string;
    name?: string;
    category?: string;
    tags?: string[];
    introduction?: string;
    spaceId?: number | string;
  };

  type UserLoginRequest = {
    useraccount?: string;
    userpassword?: string;
  };

  type UserPagesVO = {
    userList?: UserVO[];
    totalSize?: number | string;
  };

  type UserVO = {
    id?: number | string;
    username?: string;
    useraccount?: string;
    avatarurl?: string;
    gender?: number;
    phone?: string;
    email?: string;
    createtime?: string;
    updatetime?: string;
    spaceId?: number | string;
    profile?: string;
    userLevel?: string;
    userStatus?: number;
  };
}
