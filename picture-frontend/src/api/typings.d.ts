declare namespace API {
  type AddUserRequest = {
    gender?: number;
    phone?: string;
    useraccount?: string;
    username?: string;
    userstatus?: string;
  };

  type AdminCheckPictureBatchRequest = {
    checkMessage?: string;
    checkResult?: number;
    picIds?: number[];
  };

  type AdminCheckPictureRequest = {
    checkMessage?: string;
    checkResult?: number;
    picId?: number;
  };

  type AlterLevelRequest = {
    alterLevel?: number;
    spaceId?: number;
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

  type BaseResponseListUser_ = {
    code?: number;
    data?: User[];
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

  type BaseResponseUser_ = {
    code?: number;
    data?: User;
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
    spaceId?: number;
  };

  type DeleteRequest = {
    id?: number;
    ids?: number[];
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
    total?: number;
  };

  type Picture = {
    category?: string;
    checkAdminId?: number;
    checkMessage?: string;
    checkTime?: string;
    createtime?: string;
    edittime?: string;
    id?: number;
    introduction?: string;
    isdelete?: number;
    name?: string;
    picformat?: string;
    picheight?: number;
    picscale?: number;
    picsize?: number;
    pictureCheck?: number;
    picwidth?: number;
    spaceId?: number;
    tags?: string;
    thumbnailUrl?: string;
    updatetime?: string;
    url?: string;
    userid?: number;
  };

  type PicturePageVO = {
    pictureList?: PictureVO[];
    total?: number;
  };

  type PictureQueryRequest = {
    category?: string;
    current?: number;
    id?: number;
    introduction?: string;
    name?: string;
    pageSize?: number;
    pictureCheck?: number;
    searchText?: string;
    sortFiled?: string;
    sortOrder?: string;
    spaceId?: number;
    tags?: string[];
    userId?: number;
  };

  type PictureTagCategory = {
    categorys?: string[];
    tags?: string[];
  };

  type PictureUpdateRequest = {
    category?: string;
    id?: number;
    introduction?: string;
    name?: string;
    spaceId?: number;
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
    checkAdminId?: number;
    checkMessage?: string;
    createdUser?: User;
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
    spaceId?: number;
    tags?: string[];
    thumbnailUrl?: string;
    updatetime?: string;
    url?: string;
    userId?: number;
  };

  type QueryPageRequest = {
    current?: number;
    gender?: number;
    id?: number;
    profile?: string;
    queryUserAccount?: string;
    queryUsername?: string;
    size?: number;
    sortField?: string;
    sortOrder?: string;
    userStatus?: string;
  };

  type querySpaceByIdUsingGETParams = {
    /** spaceId */
    spaceId?: number;
  };

  type querySpacePageUsingGETParams = {
    current?: number;
    id?: number;
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
    id?: number;
    isDelete?: number;
    maxCount?: number;
    maxSize?: number;
    spaceLevel?: number;
    spaceName?: string;
    updateTime?: string;
    usedCount?: number;
    usedSize?: number;
    userId?: number;
  };

  type SpacePageVO = {
    spaceVOList?: SpaceVO[];
    total?: number;
  };

  type SpaceUpdateRequest = {
    spaceId?: number;
    updatedName?: string;
  };

  type SpaceVO = {
    createTime?: string;
    createdUser?: User;
    id?: number;
    maxCount?: number;
    maxSize?: number;
    spaceLevel?: number;
    spaceName?: string;
    updateTime?: string;
    usedCount?: number;
    usedSize?: number;
    userId?: number;
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
    id?: number;
    phone?: string;
    profile?: string;
    userStatus?: string;
    username?: string;
  };

  type uploadPicUsingPOSTParams = {
    /** category */
    category?: string;
    /** id */
    id?: number;
    /** introduction */
    introduction?: string;
    /** name */
    name?: string;
    /** spaceId */
    spaceId?: number;
    /** tags */
    tags?: string[];
    /** url */
    url?: string;
  };

  type User = {
    avatarurl?: string;
    createtime?: string;
    email?: string;
    gender?: number;
    id?: number;
    isdelete?: number;
    phone?: string;
    profile?: string;
    spaceId?: number;
    updatetime?: string;
    useraccount?: string;
    username?: string;
    userpassword?: string;
    userstatus?: string;
  };

  type UserLoginRequest = {
    useraccount?: string;
    userpassword?: string;
  };

  type UserPagesVO = {
    totalSize?: number;
    userList?: User[];
  };
}
