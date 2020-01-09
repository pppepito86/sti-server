//const REST_URL = "http://35.158.118.93:8081/api/";
const REST_URL = "http://localhost/api/";

export async function json(url) {
    const response = await sendRequest(url, "json");
    return await response.json();
}

export async function blob(url) {
    const response = await sendRequest(url, "blob");
    return await response.blob();
}

export async function post(url, formData) {
    const response = await sendPostRequest(url, formData);
    return await response.json();
}

async function sendRequest(url, type) {
    const token = localStorage.getItem("token");
    return sendRequestWithToken(url, type, token);
}

export function sendRequestWithToken(url, type, token) {
    return fetch(REST_URL + url, {
        headers: {
            'Authorization': `Basic ${token}`
        },
        responseType: type
    });
}

async function sendPostRequest(url, formData) {
    const token = localStorage.getItem("token");
    return fetch(REST_URL + url, {
        method: 'POST',
        headers: {
            'Authorization': `Basic ${token}`
        },
        body: formData,
        responseType: 'json'
    });
}
