package utils

import "github.com/gin-gonic/gin"

type ErrorResponse struct {
	Error   string `json:"error"`
	Code    string `json:"code"`
	Message string `json:"message"`
}

type SuccessResponse struct {
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}

func RespondError(c *gin.Context, httpStatus int, code, message string) {
	c.JSON(httpStatus, ErrorResponse{
		Error:   code,
		Code:    code,
		Message: message,
	})
}

func RespondSuccess(c *gin.Context, httpStatus int, message string, data interface{}) {
	c.JSON(httpStatus, SuccessResponse{
		Message: message,
		Data:    data,
	})
}
