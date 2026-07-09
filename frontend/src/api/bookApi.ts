import type { Book, BookCategory, ChessFile } from "../types/chess.types.ts";
import { apiClient } from './httpClient.ts'

export const getBooks = async():Promise<Book[]> =>{
    const {data} = await apiClient.get('/books');
    return data;
}

export const getChessFile = async (bookId:string):Promise<ChessFile> =>{
    const {data} = await apiClient.get(`/books/${bookId}/chess`);
    return data;
}

export const getPageImageBlob = async (bookId: string, pageNumber: number): Promise<Blob> => {
    const { data } = await apiClient.get(`/books/${bookId}/pages/${pageNumber}`, {
        responseType: 'blob'
    })
    return data
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

export const updateBookCategory = async (bookId: string, category: BookCategory): Promise<Book> => {
    const { data } = await apiClient.patch(`/books/${bookId}/category`, { category })
    return data
}

export const analyzePage = async (bookId: string, pageNumber: number): Promise<ChessFile> => {
    const { data } = await apiClient.post(`/books/${bookId}/pages/${pageNumber}/analyze`)
    return data
}