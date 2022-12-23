import React from 'react';
import ReactDOM from 'react-dom/client';

import {
    createBrowserRouter,
    RouterProvider,
} from "react-router-dom";

import './index.css';
import App from './App.jsx';

const router = createBrowserRouter([
    {
        path: "/:symbol",
        element: <App/>,
    },
    {
        path: "/",
        element: <div>Hello</div>
    }
]);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <RouterProvider router={router}/>
);
