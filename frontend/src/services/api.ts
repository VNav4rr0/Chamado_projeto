export interface ApiConfig {
  baseUrl: string;
}

export const DEFAULT_API_CONFIG: ApiConfig = {
  baseUrl: 'http://localhost:8080/api',
};

class ApiClient {
  private config: ApiConfig = { ...DEFAULT_API_CONFIG };

  public setConfig(newConfig: Partial<ApiConfig>) {
    this.config = { ...this.config, ...newConfig };
  }

  public getConfig(): ApiConfig {
    return { ...this.config };
  }

  public async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${this.config.baseUrl}${endpoint}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 12000);

    try {
      const response = await fetch(url, {
        ...options,
        headers,
        credentials: 'include', // Essencial: trafega o cookie HttpOnly tokenSuporteTi
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        let mensagemErro = response.statusText;
        try {
          const jsonErro = await response.json();
          if (jsonErro && jsonErro.erro) {
            mensagemErro = jsonErro.erro;
          } else if (typeof jsonErro === 'string') {
            mensagemErro = jsonErro;
          }
        } catch {
          const texto = await response.text().catch(() => '');
          if (texto) mensagemErro = texto;
        }

        const erroCustomizado: any = new Error(mensagemErro);
        erroCustomizado.status = response.status;
        throw erroCustomizado;
      }

      if (response.status === 204) {
        return {} as T;
      }

      return (await response.json()) as T;
    } catch (error: any) {
      clearTimeout(timeoutId);
      if (error.name === 'AbortError') {
        throw new Error('Tempo limite da requisição excedido (Timeout)');
      }
      throw error;
    }
  }

  public get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  public post<T>(endpoint: string, body: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  }

  public put<T>(endpoint: string, body: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  }

  public patch<T>(endpoint: string, body?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  public delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const api = new ApiClient();
