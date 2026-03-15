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
