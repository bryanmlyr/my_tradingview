import express from "express";
import cors from "cors";

import { stub } from "./requester";

const app = express();

app.use(cors());

const wrapper = (symbol: string) => new Promise((resolve, reject) => {
    stub.requestTimeSeries({symbol}, (err: any, value: unknown) => {
        if (err) {
            return reject(err)
        }
        resolve(value)
    })
})

app.get('/:symbol', async (req, res) => {
    const jsonValue = await wrapper(req.params.symbol);

    res.json(jsonValue);
})

app.listen(3000)
