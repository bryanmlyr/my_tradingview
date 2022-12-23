const PROTO_PATH = `${__dirname}/../proto/requester.proto`;

import {loadPackageDefinition, credentials} from '@grpc/grpc-js';
import {loadSync} from '@grpc/proto-loader';

const packageDefinition = loadSync(PROTO_PATH, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true
});

const protoDescriptor = loadPackageDefinition(packageDefinition);

// @ts-ignore
const requester = protoDescriptor.requester;

// @ts-ignore
export const stub = new requester.RequesterService('localhost:8080', credentials.createInsecure());
