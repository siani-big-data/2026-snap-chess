import axios from 'axios'
import type {Book, ChessFile} from "../types/chess.types.ts";

const apiClient = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

export const getBooks = async():Promise<Book[]> =>{
    const {data} = await apiClient.get('/books');
    return data;
}

export const getChessFile = async (bookId:string):Promise<ChessFile> =>{
    const {data} = await apiClient.get(`/books/${bookId}/chess`);
    return data;
}

export const getPageImageUrl = (bookId:string, pageNumber:number):string =>{
    return `${apiClient.defaults.baseURL}/books/${bookId}/pages/${pageNumber}`;
}

export const importBook = async (file:File, title:string):Promise<Book> =>{
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    const {data} = await apiClient.post('/books', formData,{
        headers: {'Content-Type': 'multipart/form-data'}
    });
    return data;
}
export const deleteBook = async (bookId: string): Promise<void> => {
    await apiClient.delete(`/books/${bookId}`)
}
export const renameBook = async (bookId: string, newTitle: string): Promise<Book> => {
    const { data } = await apiClient.patch(`/books/${bookId}/title`, { title: newTitle })
    return data
}
export const analyzePage = (bookId: string, pageNumber: number): Promise<ChessFile> =>
    fetch(`/api/books/${bookId}/pages/${pageNumber}/analyze`, { method: 'POST' })
        .then(res => {
            if (!res.ok) throw new Error(`Error al analizar la página ${pageNumber}`);
            return res.json();
        });
