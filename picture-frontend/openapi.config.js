import { generateService } from '@umijs/openapi';

await generateService({
  requestLibPath: "import request from '../plugin/request'",
  schemaPath: 'http://localhost:8123/v3/api-docs',
  serversPath: './src',
});
