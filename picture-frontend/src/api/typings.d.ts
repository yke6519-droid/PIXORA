declare namespace API {
  type AddUserRequest = {
    gender?: number;
    phone?: string;
    useraccount?: string;
    username?: string;
    userLevel?: string;
  };

  type AdminCheckPictureBatchRequest = {
    checkMessage?: string;
    checkResult?: number;
    /** Java Long 由后端 JsonConfig 序列化为字符串，前端不能转成 Number。 */
    picIds?: Array<number | string>;
  };

  type AdminCheckPictureRequest = {
    checkMessage?: string;
    checkResult?: number;
    /** Java Long 由后端 JsonConfig 序列化为字符串，前端不能转成 Number。 */
    picId?: number | string;
  };

  type AlterLevelRequest = {
    alterLevel?: number;
    spaceId?: number | string;
  };

  type BaseResponseBoolean_ = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseIPagePicture_ = {
    code?: number;
    data?: IPagePicture_;
    message?: string;
  };

  type BaseResponseListUserVO_ = {
    code?: number;
    data?: UserVO[];
    message?: string;
  };

  type BaseResponseListPictureVO_ = {
    code?: number;
    data?: PictureVO[];
    message?: string;
  };

  /** 批量抓图接口返回的结构化结果。 */
  type BaseResponsePictureListVO_ = {
    code?: number;
    data?: PictureListVO;
    message?: string;
  };

  type BaseResponsePicturePageVO_ = {
    code?: number;
    data?: PicturePageVO;
    message?: string;
  };

  type BaseResponsePictureTagCategory_ = {
    code?: number;
    data?: PictureTagCategory;
    message?: string;
  };

  type BaseResponsePictureVO_ = {
    code?: number;
    data?: PictureVO;
    message?: string;
  };

  type BaseResponseSpace_ = {
    code?: number;
    data?: Space;
    message?: string;
  };

  type BaseResponseSpacePageVO_ = {
    code?: number;
    data?: SpacePageVO;
    message?: string;
  };

  type BaseResponseSpaceVO_ = {
    code?: number;
    data?: SpaceVO;
    message?: string;
  };

  type BaseResponseString_ = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseUserVO_ = {
    code?: number;
    data?: UserVO;
    message?: string;
  };

  type BaseResponseUserPagesVO_ = {
    code?: number;
    data?: UserPagesVO;
    message?: string;
  };

  type CreateSpaceRequest = {
    spaceName?: string;
  };

  type deleteByIdUsingDELETEParams = {
    /** spaceId */
    spaceId?: number | string;
  };

  type DeleteRequest = {
    id?: number | string;
    ids?: Array<number | string>;
  };

  type getPictureByIdUsingGETParams = {
    /** id */
    id?: number | string;
  };

  type getUserByIdUsingGETParams = {
    id?: number;
  };

  type IPagePicture_ = {
    current?: number;
    pages?: number;
    records?: Picture[];
    size?: number;
    total?: number | string;
  };

  type Picture = {
    category?: string;
    checkAdminId?: number | string;
    checkMessage?: string;
    checkTime?: string;
    createtime?: string;
    edittime?: string;
    id?: number | string;
    introduction?: string;
    isdelete?: number;
    name?: string;
    picformat?: string;
    picheight?: number;
    picscale?: number;
    picsize?: number;
    pictureCheck?: number;
    picwidth?: number;
    spaceId?: number | string;
    tags?: string;
    thumbnailUrl?: string;
    updatetime?: string;
    url?: string;
    userid?: number | string;
  };

  type PicturePageVO = {
    pictureList?: PictureVO[];
    total?: number | string;
  };

  type PictureListVO = {
    pictureList?: PictureVO[];
    targetCount?: number;
    successCount?: number;
  };

  type PictureQueryRequest = {
    category?: string;
    current?: number;
    id?: number | string;
    introduction?: string;
    name?: string;
    pageSize?: number;
    pictureCheck?: number;
    searchText?: string;
    sortFiled?: string;
    sortOrder?: string;
    spaceId?: number | string;
    tags?: string[];
    userId?: number | string;
  };

  type PictureTagCategory = {
    categorys?: string[];
    tags?: string[];
  };

  type PictureUpdateRequest = {
    category?: string;
    id?: number | string;
    introduction?: string;
    name?: string;
    spaceId?: number | string;
    tags?: string[];
  };

  type PictureUploadByBatchRequest = {
    category?: string;
    count?: number;
    name?: string;
    searchText?: string;
    tags?: string[];
  };

  type PictureVO = {
    category?: string;
    checkAdminId?: number | string;
    checkMessage?: string;
    createdUser?: UserVO;
    createtime?: string;
    id?: number | string;
    introduction?: string;
    name?: string;
    picformat?: string;
    picheight?: number;
    picscale?: number;
    picsize?: number;
    pictureCheck?: number;
    picwidth?: number;
    spaceId?: number | string;
    tags?: string[];
    thumbnailUrl?: string;
    updatetime?: string;
    url?: string;
    userId?: number | string;
  };

  type QueryPageRequest = {
    current?: number;
    gender?: number;
    id?: number | string;
    profile?: string;
    queryUserAccount?: string;
    queryUsername?: string;
    size?: number;
    sortField?: string;
    sortOrder?: string;
    userLevel?: string;
    accountStatus?: number;
  };

  type querySpaceByIdUsingGETParams = {
    /** spaceId */
    spaceId?: number | string;
  };

  type querySpacePageUsingGETParams = {
    current?: number;
    id?: number | string;
    pageSize?: number;
    sortFiled?: string;
    sortOrder?: string;
    spaceLevel?: number;
    spaceName?: string;
  };

  type RegisterRequest = {
    gender?: number;
    phone?: string;
    reUserPassword?: string;
    useraccount?: string;
    username?: string;
    userpassword?: string;
  };

  type Space = {
    createTime?: string;
    id?: number | string;
    isDelete?: number;
    maxCount?: number | string;
    maxSize?: number | string;
    spaceLevel?: number;
    spaceName?: string;
    updateTime?: string;
    usedCount?: number | string;
    usedSize?: number | string;
    userId?: number | string;
  };

  type SpacePageVO = {
    spaceVOList?: SpaceVO[];
    total?: number | string;
  };

  type SpaceUpdateRequest = {
    spaceId?: number | string;
    updatedName?: string;
  };

  type SpaceVO = {
    createTime?: string;
    createdUser?: UserVO;
    id?: number | string;
    maxCount?: number | string;
    maxSize?: number | string;
    spaceLevel?: number;
    spaceName?: string;
    updateTime?: string;
    usedCount?: number | string;
    usedSize?: number | string;
    userId?: number | string;
  };

  type testDownloadFileUsingGETParams = {
    /** filename */
    filename?: string;
  };

  type UpdateSelfRequest = {
    avatarurl?: string;
    email?: string;
    gender?: number;
    phone?: string;
    profile?: string;
    username?: string;
  };

  type UpdateUserRequest = {
    avatarurl?: string;
    email?: string;
    gender?: number;
    id?: number | string;
    phone?: string;
    profile?: string;
    userLevel?: string;
    accountStatus?: number;
    username?: string;
  };

  type uploadPicUsingPOSTParams = {
    /** category */
    category?: string;
    /** id */
    id?: number | string;
    /** introduction */
    introduction?: string;
    /** name */
    name?: string;
    /** spaceId */
    spaceId?: number | string;
    /** tags */
    tags?: string[];
    /** url */
    url?: string;
  };

  type UserLoginRequest = {
    useraccount?: string;
    userpassword?: string;
  };

  type UserPagesVO = {
    totalSize?: number | string;
    userList?: UserVO[];
  };

  type UserVO = {
    avatarurl?: string;
    createtime?: string;
    email?: string;
    gender?: number;
    id?: number | string;
    phone?: string;
    profile?: string;
    spaceId?: number | string;
    updatetime?: string;
    useraccount?: string;
    username?: string;
    userLevel?: string;
    userStatus?: number;
  };
}
