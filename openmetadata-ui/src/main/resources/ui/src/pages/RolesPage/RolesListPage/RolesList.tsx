/*
 *  Copyright 2022 Collate.
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

import { Button, Popover, Space, Table, Tag, Tooltip } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import DeleteWidgetModal from 'components/common/DeleteWidget/DeleteWidgetModal';
import RichTextEditorPreviewer from 'components/common/rich-text-editor/RichTextEditorPreviewer';
import { usePermissionProvider } from 'components/PermissionProvider/PermissionProvider';
import { ResourceEntity } from 'components/PermissionProvider/PermissionProvider.interface';
import { isEmpty, isUndefined, uniqueId } from 'lodash';
import React, { FC, useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { getEntityName } from 'utils/EntityUtils';
import { ReactComponent as IconDelete } from '../../../assets/svg/ic-delete.svg';
import {
  NO_PERMISSION_FOR_ACTION,
  NO_PERMISSION_TO_VIEW,
} from '../../../constants/HelperTextUtil';
import { EntityType } from '../../../enums/entity.enum';
import { Operation } from '../../../generated/entity/policies/policy';
import { Role } from '../../../generated/entity/teams/role';
import { Paging } from '../../../generated/type/paging';
import {
  checkPermission,
  LIST_CAP,
  userPermissions,
} from '../../../utils/PermissionsUtils';
import {
  getPolicyWithFqnPath,
  getRoleWithFqnPath,
} from '../../../utils/RouterUtils';

interface RolesListProps {
  roles: Role[];
  fetchRoles: (paging?: Paging) => void;
}

const RolesList: FC<RolesListProps> = ({ roles, fetchRoles }) => {
  const { t } = useTranslation();
  const [selectedRole, setSelectedRole] = useState<Role>();

  const { permissions } = usePermissionProvider();

  const viewPolicyPermission = useMemo(() => {
    return (
      !isEmpty(permissions) &&
      userPermissions.hasViewPermissions(ResourceEntity.POLICY, permissions)
    );
  }, [permissions]);

  const deleteRolePermission = useMemo(() => {
    return (
      !isEmpty(permissions) &&
      checkPermission(Operation.Delete, ResourceEntity.ROLE, permissions)
    );
  }, [permissions]);

  const columns: ColumnsType<Role> = useMemo(() => {
    return [
      {
        title: t('label.name'),
        dataIndex: 'name',
        width: '200px',
        key: 'name',
        render: (_, record) => (
          <Link
            className="link-hover"
            data-testid="role-name"
            to={getRoleWithFqnPath(
              encodeURIComponent(record.fullyQualifiedName ?? '')
            )}>
            {getEntityName(record)}
          </Link>
        ),
      },
      {
        title: t('label.description'),
        dataIndex: 'description',
        key: 'description',
        render: (_, record) => (
          <RichTextEditorPreviewer markdown={record?.description || ''} />
        ),
      },
      {
        title: t('label.policy-plural'),
        dataIndex: 'policies',
        width: '250px',
        key: 'policies',
        render: (_, record) => {
          const listLength = record.policies?.length ?? 0;
          const hasMore = listLength > LIST_CAP;

          return record.policies?.length ? (
            <Space wrap data-testid="policy-link" size={4}>
              {record.policies.slice(0, LIST_CAP).map((policy) =>
                viewPolicyPermission ? (
                  <Link
                    key={uniqueId()}
                    to={getPolicyWithFqnPath(policy.fullyQualifiedName || '')}>
                    {getEntityName(policy)}
                  </Link>
                ) : (
                  <Tooltip key={uniqueId()} title={NO_PERMISSION_TO_VIEW}>
                    {getEntityName(policy)}
                  </Tooltip>
                )
              )}
              {hasMore && (
                <Popover
                  className="cursor-pointer"
                  content={
                    <Space wrap size={4}>
                      {record.policies.slice(LIST_CAP).map((policy) =>
                        viewPolicyPermission ? (
                          <Link
                            key={uniqueId()}
                            to={getPolicyWithFqnPath(
                              policy.fullyQualifiedName || ''
                            )}>
                            {getEntityName(policy)}
                          </Link>
                        ) : (
                          <Tooltip
                            key={uniqueId()}
                            title={NO_PERMISSION_TO_VIEW}>
                            {getEntityName(policy)}
                          </Tooltip>
                        )
                      )}
                    </Space>
                  }
                  overlayClassName="w-40 text-center"
                  trigger="click">
                  <Tag className="m-l-xss" data-testid="plus-more-count">{`+${
                    listLength - LIST_CAP
                  } more`}</Tag>
                </Popover>
              )}
            </Space>
          ) : (
            '-- '
          );
        },
      },
      {
        title: t('label.action-plural'),
        dataIndex: 'actions',
        width: '80px',
        align: 'center',
        key: 'actions',
        render: (_, record) => {
          return (
            <Tooltip
              placement="left"
              title={!deleteRolePermission && NO_PERMISSION_FOR_ACTION}>
              <Button
                data-testid={`delete-action-${getEntityName(record)}`}
                disabled={!deleteRolePermission}
                icon={<IconDelete name={t('label.delete')} width="16px" />}
                type="text"
                onClick={() => setSelectedRole(record)}
              />
            </Tooltip>
          );
        },
      },
    ];
  }, []);

  const handleAfterDeleteAction = useCallback(() => {
    fetchRoles();
  }, [fetchRoles]);

  return (
    <>
      <Table
        bordered
        className="roles-list-table"
        columns={columns}
        data-testid="roles-list-table"
        dataSource={roles}
        pagination={false}
        rowKey="name"
        size="small"
      />
      {selectedRole && (
        <DeleteWidgetModal
          afterDeleteAction={handleAfterDeleteAction}
          allowSoftDelete={false}
          deleteMessage={t('message.are-you-sure-delete-entity', {
            entity: getEntityName(selectedRole),
          })}
          entityId={selectedRole.id}
          entityName={getEntityName(selectedRole)}
          entityType={EntityType.ROLE}
          visible={!isUndefined(selectedRole)}
          onCancel={() => setSelectedRole(undefined)}
        />
      )}
    </>
  );
};

export default RolesList;