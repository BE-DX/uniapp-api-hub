-- =========================================
-- 种子数据: K3Cloud 金蝶星空系统 + 路由
-- 开发环境: K3Cloud 部署在 localhost
-- =========================================

USE uniapp_api_hub;

-- 系统配置
INSERT INTO sys_system_config (sys_code, sys_name, base_url, auth_type, auth_config, enabled, remark) VALUES
('k3cloud', '金蝶星空K3Cloud', 'http://localhost',
 'TOKEN',
 '{"appId":"343282_713CSdvETqDaR/8K655M591s1t0+6PLG","appSec":"5d07c9454ded45b0a9fc3e4dfee45711","acctID":"67482053589790","username":"kingdee2","lcid":"2052"}',
 1, '开发环境K3Cloud');

-- API路由 (system_id=1)
INSERT INTO sys_api_route (system_id, route_key, target_path, http_method, enabled, remark) VALUES
(1, 'save',           '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc',           'POST', 1, '保存'),
(1, 'delete',         '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Delete.common.kdsvc',         'POST', 1, '删除'),
(1, 'submit',         '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Submit.common.kdsvc',         'POST', 1, '提交'),
(1, 'audit',          '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Audit.common.kdsvc',          'POST', 1, '审核'),
(1, 'unAudit',        '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.UnAudit.common.kdsvc',        'POST', 1, '反审核'),
(1, 'view',           '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc',           'POST', 1, '查看'),
(1, 'draft',          '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Draft.common.kdsvc',          'POST', 1, '暂存'),
(1, 'query',          '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc','POST', 1, '查询'),
(1, 'statusOperate',  '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExcuteOperation.common.kdsvc', 'POST', 1, '状态操作'),
(1, 'validateUser',   '/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc',         'POST', 1, '验证用户');
