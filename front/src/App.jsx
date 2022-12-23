import {useEffect, useState} from "react";
import {useParams, useSearchParams} from "react-router-dom";
import ReactECharts from 'echarts-for-react';

const Chart = ({symbol}) => {
    const [data, setData] = useState(null)

    const options = {
        xAxis: {
            type: 'category',
            data: data?.res.map(e => new Date(e.timestamp * 1000).toLocaleString())
        },
        yAxis: {
            min: 'dataMin',
            max: 'dataMax',
        },
        dataZoom: [
            {
                type: 'slider'
            },
        ],
        series: [
            {
                data: data?.res.map(e => [e?.price, e?.open, e?.low, e?.high]),
                type: 'candlestick',
            },
        ],
        tooltip: {
            trigger: 'axis',
        },
    };

    useEffect(() => {
        async function fetchData() {
            const response = await fetch(`http://localhost:3000/${symbol}`);
            const data = await response.json();
            setData(data);
        }

        const timer = setInterval(() => {
            fetchData();
        }, 1000);

        return () => clearInterval(timer);
    })

    return <ReactECharts style={{height: "100vh"}} theme={'dark'} option={options}/>;
};

function App() {
    const {symbol} = useParams();

    return (
        <Chart symbol={symbol}/>
    );
}

export default App;
