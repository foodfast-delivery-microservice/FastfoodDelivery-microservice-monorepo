import axios from "axios";

export const api = axios.create({
    baseURL: "http://localhost:5002", // dùng chung db.json của web
});
