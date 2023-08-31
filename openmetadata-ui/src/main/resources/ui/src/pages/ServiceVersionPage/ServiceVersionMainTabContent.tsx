/*
 *  Copyright 2023 Collate.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import { Col, Row, Space, Table } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import DescriptionV1 from 'components/common/description/DescriptionV1';
import ErrorPlaceHolder from 'components/common/error-with-placeholder/ErrorPlaceHolder';
import NextPrevious from 'components/common/next-previous/NextPrevious';
import Loader from 'components/Loader/Loader';
import TagsContainerV2 from 'components/Tag/TagsContainerV2/TagsContainerV2';
import { DisplayType } from 'components/Tag/TagsViewer/TagsViewer.interface';
import { PAGE_SIZE } from 'constants/constants';
import { EntityField } from 'constants/Feeds.constants';
import { TABLE_SCROLL_VALUE } from 'constants/Table.constants';
import { TagSource } from 'generated/type/tagLabel';
import { isEmpty, isNil } from 'lodash';
import { ServiceTypes } from 'Models';
import React, { useMemo } from 'react';
import { useParams } from 'react-router-dom';
import {
  getEntityVersionByField,
  getEntityVersionTags,
} from 'utils/EntityVersionUtils';
import { getServiceMainTabColumns } from 'utils/ServiceMainTabContentUtils';
import { ServicePageData } from '../ServiceDetailsPage/ServiceDetailsPage';
import { ServiceVersionMainTabContentProps } from './ServiceVersionMainTabContent.interface';

function ServiceVersionMainTabContent({
  serviceName,
  data,
  isServiceLoading,
  paging,
  pagingHandler,
  currentPage,
  serviceDetails,
  entityType,
  changeDescription,
}: ServiceVersionMainTabContentProps) {
  const { serviceFQN, serviceCategory } = useParams<{
    serviceFQN: string;
    serviceCategory: ServiceTypes;
  }>();

  const tableColumn: ColumnsType<ServicePageData> = useMemo(
    () => getServiceMainTabColumns(serviceCategory),
    [serviceCategory]
  );

  const tags = useMemo(() => {
    return getEntityVersionTags(serviceDetails, changeDescription);
  }, [serviceDetails, changeDescription]);

  const description = useMemo(() => {
    return getEntityVersionByField(
      changeDescription,
      EntityField.DESCRIPTION,
      serviceDetails.description
    );
  }, [serviceDetails, changeDescription]);

  return (
    <Row gutter={[0, 16]} wrap={false}>
      <Col className="p-t-sm m-x-lg" flex="auto">
        <Row gutter={[16, 16]}>
          <Col data-testid="description-container" span={24}>
            <DescriptionV1
              isVersionView
              description={description}
              entityFqn={serviceFQN}
              entityName={serviceName}
              entityType={entityType}
            />
          </Col>

          <Col data-testid="table-container" span={24}>
            <Space className="w-full m-b-md" direction="vertical" size="middle">
              <Table
                bordered
                columns={tableColumn}
                data-testid="service-children-table"
                dataSource={data}
                loading={{
                  spinning: isServiceLoading,
                  indicator: <Loader size="small" />,
                }}
                locale={{
                  emptyText: <ErrorPlaceHolder className="m-y-md" />,
                }}
                pagination={false}
                rowKey="name"
                scroll={TABLE_SCROLL_VALUE}
                size="small"
              />

              {Boolean(!isNil(paging.after) || !isNil(paging.before)) &&
                !isEmpty(data) && (
                  <NextPrevious
                    currentPage={currentPage}
                    pageSize={PAGE_SIZE}
                    paging={paging}
                    pagingHandler={pagingHandler}
                    totalCount={paging.total}
                  />
                )}
            </Space>
          </Col>
        </Row>
      </Col>
      <Col
        className="entity-tag-right-panel-container"
        data-testid="entity-right-panel"
        flex="220px">
        <Space className="w-full" direction="vertical" size="large">
          {Object.keys(TagSource).map((tagType) => (
            <TagsContainerV2
              displayType={DisplayType.READ_MORE}
              entityFqn={serviceFQN}
              entityType={entityType}
              key={tagType}
              permission={false}
              selectedTags={tags}
              showTaskHandler={false}
              tagType={TagSource[tagType as TagSource]}
            />
          ))}
        </Space>
      </Col>
    </Row>
  );
}

export default ServiceVersionMainTabContent;